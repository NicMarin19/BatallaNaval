package com.batallanaval.view;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;

/**
 * Generador procedural de texturas. Todas las texturas "tileables" usan
 * armonicos enteros para que el patron sea perfectamente continuo al repetirse.
 */
public class TextureGenerator {

    private static final double TAU = Math.PI * 2.0;

    // ------------------------------------------------------------------
    //  AGUA
    // ------------------------------------------------------------------

    /** Textura base del oceano profundo (opaca). */
    public static Texture2D createWaterTexture(int width, int height) {
        return createWaterTexture(width, height,
                new ColorRGBA(0.004f, 0.030f, 0.075f, 1f),
                new ColorRGBA(0.012f, 0.115f, 0.210f, 1f),
                new ColorRGBA(0.240f, 0.470f, 0.620f, 1f),
                255, 0.0);
    }

    /**
     * Textura de oleaje seamless. Permite generar capas con distinta paleta y
     * nivel de transparencia para animar el mar por scroll de UV.
     */
    public static Texture2D createWaterTexture(int width, int height,
                                               ColorRGBA deep, ColorRGBA mid, ColorRGBA crest,
                                               int alpha, double phase) {
        ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
        int seed = 1000 + (int) Math.round(phase * 97.0);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double u = (double) x / width;
                double v = (double) y / height;

                // fBm anisotropo: manchas de oleaje alargadas, sin bandas rectas.
                double broad = fbmAniso(u, v, 3, 7, 3, seed);
                double medium = fbmAniso(u, v, 8, 16, 2, seed + 17);
                double fine = fbmAniso(u, v, 18, 31, 2, seed + 43);

                // Tren de olas cuya fase deforma el ruido para romper la repeticion.
                double warp = (broad - 0.5) * 3.2;
                double swell = 0.5 + 0.5 * Math.sin(TAU * (5.0 * u + 2.0 * v) + warp + phase);
                swell = Math.pow(swell, 1.7);

                double wave = clamp01(0.34 * broad + 0.34 * swell
                        + 0.20 * medium + 0.12 * fine);

                // Solo las zonas mas altas generan cresta.
                double crestAmount = clamp01((wave - 0.70) / 0.30);
                crestAmount *= crestAmount;

                // Chispas capilares muy dispersas.
                double sparkle = clamp01((fine - 0.80) / 0.20);
                sparkle *= sparkle * 0.55;

                ColorRGBA c = lerp(deep, mid, (float) clamp01(wave * 1.25));
                c = lerp(c, crest, (float) clamp01(crestAmount * 0.55 + sparkle));

                int a = (int) Math.round(alpha * (0.86 + 0.14 * wave));
                a = Math.min(255, Math.max(0, a));

                buf.put((byte) toByte(c.r)).put((byte) toByte(c.g))
                        .put((byte) toByte(c.b)).put((byte) a);
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, width, height, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    /** Espuma fina transparente para superponer en modo aditivo. */
    public static Texture2D createFoamTexture(int size, int alpha) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double u = (double) x / size;
                double v = (double) y / size;

                // Parches de espuma irregulares en lugar de lineas repetidas.
                double patches = fbmAniso(u, v, 4, 9, 3, 211);
                double lanes = fbmAniso(u, v, 14, 5, 2, 307);

                double lace = clamp01((patches * 0.65 + lanes * 0.35 - 0.58) / 0.42);
                lace = Math.pow(lace, 2.4);

                int a = (int) Math.round(lace * alpha);
                buf.put((byte) 205).put((byte) 240).put((byte) 250).put((byte) a);
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    /**
     * Estela de espuma para la popa de los barcos decorativos.
     * u = ancho de la estela, v = largo (v=0 popa, v=1 extremo lejano).
     */
    public static Texture2D createWakeTexture(int width, int height) {
        ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double u = (double) x / (width - 1);
                double v = (double) y / (height - 1);

                double fade = Math.pow(1.0 - v, 1.6);
                double dx = Math.abs(u - 0.5);

                // Dos lineas divergentes (V de estela)
                double spread = 0.035 + 0.44 * v;
                double arm = Math.exp(-Math.pow((dx - spread) / 0.045, 2.0));

                // Nucleo central turbulento
                double coreWidth = 0.040 * (1.0 - v) + 0.020;
                double core = Math.exp(-Math.pow(dx / coreWidth, 2.0)) * 0.55;

                double alpha = clamp01(arm + core) * fade;

                buf.put((byte) 210).put((byte) 242).put((byte) 252)
                        .put((byte) Math.round(alpha * 215));
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, width, height, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    // ------------------------------------------------------------------
    //  CIELO / MENUS
    // ------------------------------------------------------------------

    /** Degradado vertical para la cupula del cielo (abajo -> arriba). */
    public static Texture2D createSkyGradientTexture(int height) {
        int width = 8;
        ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);

        ColorRGBA belowHorizon = new ColorRGBA(0.045f, 0.120f, 0.190f, 1f);
        ColorRGBA horizon = new ColorRGBA(0.440f, 0.640f, 0.760f, 1f);
        ColorRGBA upper = new ColorRGBA(0.110f, 0.290f, 0.480f, 1f);
        ColorRGBA zenith = new ColorRGBA(0.015f, 0.055f, 0.140f, 1f);

        for (int y = 0; y < height; y++) {
            double t = (double) y / (height - 1); // 0 abajo, 1 arriba
            ColorRGBA c;
            if (t < 0.48) {
                c = lerp(belowHorizon, horizon, (float) (t / 0.48));
            } else if (t < 0.62) {
                c = lerp(horizon, upper, (float) ((t - 0.48) / 0.14));
            } else {
                double k = (t - 0.62) / 0.38;
                c = lerp(upper, zenith, (float) Math.pow(k, 0.8));
            }
            for (int x = 0; x < width; x++) {
                buf.put((byte) toByte(c.r)).put((byte) toByte(c.g))
                        .put((byte) toByte(c.b)).put((byte) 255);
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, width, height, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    /** Resplandor radial blanco/calido para el sol y brillos sobre el agua. */
    public static Texture2D createGlowTexture(int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        float center = size * 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - center) / center;
                float dy = (y - center) / center;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - d);
                a = a * a * a;
                buf.put((byte) 255).put((byte) 240).put((byte) 205)
                        .put((byte) Math.round(a * 230));
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    /** Degradado horizontal con alfa, usado como fondo de los menus. */
    public static Texture2D createHorizontalGradientTexture(int width, int height,
                                                           ColorRGBA left, ColorRGBA right) {
        ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float t = (float) x / (width - 1);
                t = t * t * (3f - 2f * t); // smoothstep
                ColorRGBA c = lerp(left, right, t);
                buf.put((byte) toByte(c.r)).put((byte) toByte(c.g))
                        .put((byte) toByte(c.b)).put((byte) toByte(c.a));
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, width, height, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    /** Textura de radar decorativa con anillos, cruz y barrido. */
    public static Texture2D createRadarTexture(int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        float c = size * 0.5f;
        float maxR = size * 0.5f;
        float sweepAngle = -0.55f;
        float sweepWidth = 0.55f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c;
                float dy = y - c;
                float dist = (float) Math.sqrt(dx * dx + dy * dy) / maxR;

                float alpha = 0f;
                if (dist <= 0.98f) {
                    // Anillos
                    for (float ringRadius : new float[]{0.30f, 0.60f, 0.90f}) {
                        float ring = Math.abs(dist - ringRadius);
                        if (ring < 0.010f) alpha = Math.max(alpha, 0.55f);
                    }
                    // Cruz central
                    if (Math.abs(dx) < 1.2f || Math.abs(dy) < 1.2f) alpha = Math.max(alpha, 0.30f);

                    // Barrido giratorio con degradado angular
                    float ang = (float) Math.atan2(dy, dx);
                    float delta = angDiff(ang, sweepAngle);
                    if (delta < sweepWidth) {
                        float sweep = (1f - delta / sweepWidth);
                        sweep *= sweep * 0.55f * (1f - dist * 0.55f);
                        alpha = Math.max(alpha, sweep);
                    }

                    // Ticks del borde
                    if (dist > 0.93f && ((Math.abs(dx) < 2.0f || Math.abs(dy) < 2.0f))) {
                        alpha = Math.max(alpha, 0.7f);
                    }
                }

                int a = (int) (clamp01(alpha) * 255);
                buf.put((byte) 120).put((byte) 220).put((byte) 255).put((byte) a);
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    // ------------------------------------------------------------------
    //  DADOS
    // ------------------------------------------------------------------

    /** Cara de dado procedural (fondo marfil y pips oscuros). */
    public static Texture2D createDiceFaceTexture(int face, int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        double margin = size * 0.06;
        double inner = size - margin * 2.0;
        double pipRadius = size * 0.085;

        double[][] pips;
        switch (face) {
            case 1:
                pips = new double[][]{{0.5, 0.5}};
                break;
            case 2:
                pips = new double[][]{{0.25, 0.25}, {0.75, 0.75}};
                break;
            case 3:
                pips = new double[][]{{0.25, 0.25}, {0.5, 0.5}, {0.75, 0.75}};
                break;
            case 4:
                pips = new double[][]{{0.25, 0.25}, {0.75, 0.25}, {0.25, 0.75}, {0.75, 0.75}};
                break;
            case 5:
                pips = new double[][]{{0.25, 0.25}, {0.75, 0.25}, {0.5, 0.5}, {0.25, 0.75}, {0.75, 0.75}};
                break;
            default:
                pips = new double[][]{{0.25, 0.25}, {0.75, 0.25}, {0.25, 0.5},
                                      {0.75, 0.5}, {0.25, 0.75}, {0.75, 0.75}};
                break;
        }

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double px = x + 0.5;
                double py = y + 0.5;

                boolean inside = px >= margin && px <= size - margin
                        && py >= margin && py <= size - margin;

                // Esquinas redondeadas
                if (inside) {
                    double cornerR = size * 0.10;
                    double cx = Math.min(Math.max(px, margin + cornerR), size - margin - cornerR);
                    double cy = Math.min(Math.max(py, margin + cornerR), size - margin - cornerR);
                    double d = Math.hypot(px - cx, py - cy);
                    if (d > cornerR) inside = false;
                }

                if (!inside) {
                    buf.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                    continue;
                }

                boolean isPip = false;
                for (double[] pip : pips) {
                    double pipX = margin + inner * pip[0];
                    double pipY = margin + inner * pip[1];
                    if (Math.hypot(px - pipX, py - pipY) <= pipRadius) {
                        isPip = true;
                        break;
                    }
                }

                if (isPip) {
                    buf.put((byte) 22).put((byte) 26).put((byte) 32).put((byte) 255);
                } else {
                    buf.put((byte) 242).put((byte) 238).put((byte) 224).put((byte) 255);
                }
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    // ------------------------------------------------------------------
    //  ICONOS DE INTERFAZ
    // ------------------------------------------------------------------

    /**
     * Icono de comunicacion por voz: insignia circular dorada con un
     * bocadillo de dialogo y tres ondas de sonido. Se usa en la tarjeta
     * que muestra las frases al hundir un barco.
     */
    public static Texture2D createVoiceIconTexture(int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);

        float center = size * 0.5f;
        float radius = size * 0.47f;
        float border = Math.max(2f, size * 0.035f);

        float bubbleCx = size * 0.41f;
        float bubbleCy = size * 0.525f;
        float bubbleHalfW = size * 0.21f;
        float bubbleHalfH = size * 0.15f;
        float bubbleCorner = size * 0.055f;

        float waveCx = size * 0.715f;
        float waveCy = size * 0.52f;
        float waveThickness = Math.max(1.4f, size * 0.016f);
        float[] waveRadii = {size * 0.075f, size * 0.135f, size * 0.195f};

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                float dx = px - center;
                float dy = py - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist > radius) {
                    buf.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                    continue;
                }

                // Distancia con signo a un rectangulo redondeado (bocadillo).
                float bx = Math.abs(px - bubbleCx) - (bubbleHalfW - bubbleCorner);
                float by = Math.abs(py - bubbleCy) - (bubbleHalfH - bubbleCorner);
                float outsideX = Math.max(bx, 0f);
                float outsideY = Math.max(by, 0f);
                float bubbleDistance = (float) Math.sqrt(outsideX * outsideX + outsideY * outsideY)
                        + Math.min(Math.max(bx, by), 0f) - bubbleCorner;
                boolean bubble = bubbleDistance <= 0f;

                // Cola triangular del bocadillo.
                float tailLeft = size * 0.255f;
                float tailRight = size * 0.405f;
                float tailTop = size * 0.385f;
                float tailBottom = size * 0.245f;
                boolean tail = py >= tailBottom && py <= tailTop && px >= tailLeft
                        && px <= tailLeft + (tailTop - py) * ((tailRight - tailLeft) / (tailTop - tailBottom));

                // Ondas de sonido a la derecha del bocadillo.
                boolean wave = false;
                if (px > waveCx) {
                    float wx = px - waveCx;
                    float wy = py - waveCy;
                    float waveDist = (float) Math.sqrt(wx * wx + wy * wy);
                    for (float waveRadius : waveRadii) {
                        if (Math.abs(waveDist - waveRadius) <= waveThickness
                                && Math.abs(wy) <= waveRadius * 0.90f) {
                            wave = true;
                            break;
                        }
                    }
                }

                boolean rim = dist >= radius - border;
                if (rim || bubble || tail || wave) {
                    // Dorado calido con un nucleo mas claro.
                    boolean highlight = bubble || tail;
                    int r = highlight ? 255 : 235;
                    int g = highlight ? 224 : 195;
                    int b = highlight ? 120 : 55;
                    buf.put((byte) r).put((byte) g).put((byte) b).put((byte) 255);
                } else {
                    // Fondo azul naval profundo.
                    buf.put((byte) 8).put((byte) 24).put((byte) 44).put((byte) 255);
                }
            }
        }

        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf,
                com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    // ------------------------------------------------------------------
    //  UTILIDADES EXISTENTES
    // ------------------------------------------------------------------

    public static Texture2D createGridCellTexture(int size, ColorRGBA border, ColorRGBA fill) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        int bw = Math.max(1, size / 16);
        int cornerLen = size / 4;

        int br = (int) (border.r * 255);
        int bg = (int) (border.g * 255);
        int bb = (int) (border.b * 255);
        int ba = (int) (border.a * 255);

        int fr = (int) (fill.r * 255);
        int fg = (int) (fill.g * 255);
        int fb = (int) (fill.b * 255);
        int fa = (int) (fill.a * 255);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean isBorder = (x < bw || x >= size - bw || y < bw || y >= size - bw);

                // Reinforced tactical corner brackets
                boolean isCorner = (x < cornerLen && (y < bw * 2 || y >= size - bw * 2)) ||
                                   (x >= size - cornerLen && (y < bw * 2 || y >= size - bw * 2)) ||
                                   (y < cornerLen && (x < bw * 2 || x >= size - bw * 2)) ||
                                   (y >= size - cornerLen && (x < bw * 2 || x >= size - bw * 2));

                // Central laser reticle crosshair
                int cx = size / 2;
                int cy = size / 2;
                boolean isCross = (Math.abs(x - cx) <= 1 && Math.abs(y - cy) <= 4) ||
                                  (Math.abs(y - cy) <= 1 && Math.abs(x - cx) <= 4);

                // Subtle inner radar scanline
                float scanline = (y % 4 == 0) ? 0.92f : 1.0f;

                if (isCorner) {
                    // Bright accent corner bracket
                    int cr = Math.min(255, br + 40);
                    int cg = Math.min(255, bg + 40);
                    int cb = Math.min(255, bb + 40);
                    buf.put((byte) cr).put((byte) cg).put((byte) cb).put((byte) 255);
                } else if (isBorder) {
                    buf.put((byte) br).put((byte) bg).put((byte) bb).put((byte) ba);
                } else if (isCross) {
                    int cr = Math.min(255, (br + fr) / 2 + 50);
                    int cg = Math.min(255, (bg + fg) / 2 + 50);
                    int cb = Math.min(255, (bb + fb) / 2 + 50);
                    buf.put((byte) cr).put((byte) cg).put((byte) cb).put((byte) 230);
                } else {
                    int r = (int) (fr * scanline);
                    int g = (int) (fg * scanline);
                    int b = (int) (fb * scanline);
                    buf.put((byte) r).put((byte) g).put((byte) b).put((byte) fa);
                }
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf, com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    /**
     * Textura de casilla carbonizada/negra para marcar los impactos y los barcos explotados.
     */
    public static Texture2D createScorchedCellTexture(int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        int bw = Math.max(1, size / 16);
        float cx = size * 0.5f;
        float cy = size * 0.5f;
        float maxR = size * 0.48f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f) - cx;
                float dy = (y + 0.5f) - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float normDist = dist / maxR;

                boolean isBorder = (x < bw || x >= size - bw || y < bw || y >= size - bw);

                // Base negra carbonizada
                int r = 12;
                int g = 12;
                int b = 15;
                int a = 250;

                // Cráter de explosión y hollín
                if (normDist < 1.0f) {
                    float blast = 1.0f - normDist;
                    r = (int) (8 + 10 * blast);
                    g = (int) (8 + 8 * blast);
                    b = (int) (10 + 6 * blast);

                    // Anillo de brasas oscuras cerca del centro
                    if (normDist > 0.20f && normDist < 0.52f) {
                        float ringFactor = 1.0f - Math.abs(normDist - 0.36f) / 0.16f;
                        r += (int) (24 * ringFactor);
                        g += (int) (8 * ringFactor);
                    }
                }

                if (isBorder) {
                    r = 6;
                    g = 6;
                    b = 8;
                    a = 255;
                }

                int noise = ((x * 19 + y * 37) % 7) - 3;
                r = Math.max(0, Math.min(255, r + noise));
                g = Math.max(0, Math.min(255, g + noise));
                b = Math.max(0, Math.min(255, b + noise));

                buf.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf, com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    /**
     * Textura de destello / estrella dorada de mejora para la animación de dados.
     */
    public static Texture2D createUpgradeStarTexture(int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        float c = size * 0.5f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = Math.abs(x + 0.5f - c) / c;
                float dy = Math.abs(y + 0.5f - c) / c;

                // Estrella de 4 puntas suave (curva exponencial)
                float armX = (float) Math.pow(Math.max(0, 1.0f - dx), 4.0f) * (float) Math.pow(Math.max(0, 1.0f - dy * 4.0f), 2.0f);
                float armY = (float) Math.pow(Math.max(0, 1.0f - dy), 4.0f) * (float) Math.pow(Math.max(0, 1.0f - dx * 4.0f), 2.0f);
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float core = (float) Math.max(0, 1.0f - dist * 2.2f);
                core = core * core;

                float intensity = Math.min(1.0f, armX + armY + core);

                int r = 255;
                int g = (int) (180 + 75 * core);
                int b = (int) (50 + 150 * core);
                int a = (int) (intensity * 255);

                buf.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf, com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    /**
     * Textura de anillo aura / onda expansiva de mejora.
     */
    public static Texture2D createAuraRingTexture(int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        float c = size * 0.5f;
        float maxR = c * 0.95f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - c) / maxR;
                float dy = (y + 0.5f - c) / maxR;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                float alpha = 0f;
                if (dist <= 1.0f) {
                    float ringWidth = 0.16f;
                    float ringCenter = 0.78f;
                    float d = Math.abs(dist - ringCenter);
                    if (d < ringWidth) {
                        alpha = 1.0f - (d / ringWidth);
                        alpha = alpha * alpha;
                    }
                    // Inner faint glow
                    float inner = Math.max(0, 1.0f - dist);
                    alpha = Math.max(alpha, inner * 0.25f);
                }

                int r = 255;
                int g = 210;
                int b = 60;
                int a = (int) (clamp01(alpha) * 255);

                buf.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf, com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    public static Texture2D createHazardStripeTexture(int width, int height) {
        ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
        int stripeWidth = 16;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isYellow = ((x + y) / stripeWidth) % 2 == 0;
                if (isYellow) {
                    // Bright caution yellow
                    buf.put((byte) 235).put((byte) 185).put((byte) 20).put((byte) 255);
                } else {
                    // Charcoal hazard black
                    buf.put((byte) 25).put((byte) 28).put((byte) 32).put((byte) 255);
                }
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, width, height, buf, com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    public static Texture2D createTargetReticleTexture(int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        float center = size * 0.5f;
        float outerR = size * 0.44f;
        float innerR = size * 0.38f;
        float centerR = size * 0.12f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                boolean inOuterRing = (dist >= innerR && dist <= outerR);
                boolean inCenterDot = (dist <= centerR);
                boolean onCrosshairAxis = (Math.abs(dx) <= 2 || Math.abs(dy) <= 2) && (dist <= outerR + 4 && dist >= centerR * 1.5f);

                if (inOuterRing || inCenterDot || onCrosshairAxis) {
                    buf.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 240);
                } else {
                    buf.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                }
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf, com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    public static Texture2D createParticleTexture(int size) {
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        float center = size / 2f;
        float maxRadius = size / 2f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float alpha = Math.max(0f, 1f - dist / maxRadius);
                alpha = alpha * alpha; // Quadratic soft falloff

                int a = (int) (alpha * 255);
                buf.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) a);
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, size, size, buf, com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    public static Texture2D createButtonTexture(int width, int height, ColorRGBA fill, ColorRGBA border) {
        ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
        int bw = 2;

        int br = (int) (border.r * 255);
        int bg = (int) (border.g * 255);
        int bb = (int) (border.b * 255);
        int ba = (int) (border.a * 255);

        int fr = (int) (fill.r * 255);
        int fg = (int) (fill.g * 255);
        int fb = (int) (fill.b * 255);
        int fa = (int) (fill.a * 255);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isBorder = (x < bw || x >= width - bw || y < bw || y >= height - bw);

                // Subtle tactical chamfered corners
                boolean isCornerCut = (x < 3 && y < 3) || (x >= width - 3 && y < 3) ||
                                      (x < 3 && y >= height - 3) || (x >= width - 3 && y >= height - 3);

                if (isCornerCut) {
                    buf.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                    continue;
                }

                float grad = 1.0f - 0.25f * ((float) y / height);
                if (isBorder) {
                    buf.put((byte) br).put((byte) bg).put((byte) bb).put((byte) ba);
                } else {
                    int r = (int) (fr * grad);
                    int g = (int) (fg * grad);
                    int b = (int) (fb * grad);
                    buf.put((byte) r).put((byte) g).put((byte) b).put((byte) fa);
                }
            }
        }
        buf.flip();
        Image img = new Image(Image.Format.RGBA8, width, height, buf, com.jme3.texture.image.ColorSpace.sRGB);
        return new Texture2D(img);
    }

    // ------------------------------------------------------------------
    //  HELPERS
    // ------------------------------------------------------------------

    private static ColorRGBA lerp(ColorRGBA a, ColorRGBA b, float t) {
        t = clamp01(t);
        return new ColorRGBA(
                a.r + (b.r - a.r) * t,
                a.g + (b.g - a.g) * t,
                a.b + (b.b - a.b) * t,
                a.a + (b.a - a.a) * t);
    }

    private static float clamp01(double v) {
        return (float) Math.min(1.0, Math.max(0.0, v));
    }

    private static int toByte(float v) {
        return Math.min(255, Math.max(0, Math.round(v * 255f)));
    }

    private static float angDiff(float a, float b) {
        float d = a - b;
        while (d > Math.PI) d -= TAU;
        while (d < -Math.PI) d += TAU;
        return Math.abs(d);
    }

    // ------------------------------------------------------------------
    //  RUIDO PERIODICO (para texturas tileables)
    // ------------------------------------------------------------------

    /**
     * Ruido de valor con periodo entero en cada eje. Anisotropo: permite
     * estirar las manchas para simular oleaje y espuma.
     */
    private static double fbmAniso(double u, double v, int periodX, int periodY,
                                   int octaves, int seed) {
        double sum = 0.0;
        double amp = 0.58;
        double norm = 0.0;
        int px = periodX;
        int py = periodY;

        for (int i = 0; i < octaves; i++) {
            sum += amp * valueNoise(u, v, px, py, seed + i * 41);
            norm += amp;
            amp *= 0.5;
            px *= 2;
            py *= 2;
        }
        return sum / norm;
    }

    private static double valueNoise(double u, double v, int periodX, int periodY, int seed) {
        double x = u * periodX;
        double y = v * periodY;

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;

        int x1 = floorMod(x0 + 1, periodX);
        int y1 = floorMod(y0 + 1, periodY);
        x0 = floorMod(x0, periodX);
        y0 = floorMod(y0, periodY);

        double n00 = hash01(x0, y0, seed);
        double n10 = hash01(x1, y0, seed);
        double n01 = hash01(x0, y1, seed);
        double n11 = hash01(x1, y1, seed);

        // Smoothstep para transiciones suaves entre nodos.
        double sx = fx * fx * (3.0 - 2.0 * fx);
        double sy = fy * fy * (3.0 - 2.0 * fy);

        double a = n00 + (n10 - n00) * sx;
        double b = n01 + (n11 - n01) * sx;
        return a + (b - a) * sy;
    }

    private static int floorMod(int value, int mod) {
        int r = value % mod;
        return r < 0 ? r + mod : r;
    }

    /** Hash entero estable (sin Random) devuelto en el rango [0, 1]. */
    private static double hash01(int x, int y, int seed) {
        int h = x * 0x27d4eb2d ^ y * 0x165667b1 ^ seed * 0x9e3779b9;
        h ^= (h >>> 15);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        return (h & 0x7fffffff) / (double) 0x7fffffff;
    }
}
