package com.batallanaval.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Repertorio de frases espanolas que se muestran al hundir un barco enemigo.
 * Incluye expresiones de humor, jerga marinera y guinos de internet.
 */
public final class PhraseBank {

    private static final String[] PHRASES = {
            "Aparentemente 🗣️",
            "¡Impacto directo! ¡El navío se parte en dos!",
            "¡Blanco fijado y destruido por completo!",
            "¡Hundimiento visual confirmado, capitán!",
            "¡Salva certera a la línea de flotación!",
            "¡Fuego en santabárbara, explosión catastrófica!",
            "¡El buque enemigo se va a pique!",
            "¡Colapso estructural, objetivo neutralizado!",
            "¡Directo a la quilla, nave abatida!",
            "¡Agua entrando a borbotones por estribor!",
            "¡Blanco eliminado del radar de tiro!",
            "¡Excelente puntería de la batería principal!",
            "¡La nave enemiga zozobra sin remedio!",
            "¡Ese destructor ya no vuelve a la superficie!",
            "¡Impacto devastador, casco destrozado!",
            "¡Artillería reporta blanco hundido a pique!",
            "¡Reportando naufragio enemigo confirmado!",
            "¡Misión cumplida, nave enviada al fondo marino!",
            "¡Objetivo desintegrado por la andanada!",
            "¡Torpedo certero, nave enemiga aniquilada!",
            "¡Colapso del casco confirmado por sonar!",
            "¡Fuego incontrolable a bordo del enemigo!",
            "¡El buque enemigo zozobra y se sumerge!",
            "¡Confirmado por vigía: nave partida en dos!",
            "¡Salva de artillería en el blanco, se hunde!",
            "¡El enemigo se va directo al fondo del mar!",
            "¡Compartimentos inundados, hundimiento inminente!",
            "¡Impacto crítico en la sala de máquinas!",
            "¡Al lecho marino con toda su artillería!",
            "¡Buque hostil borrado de la pantalla de radar!",
            "¡Una salva impecable, navío neutralizado!",
            "¡Puntería milimétrica, blanco al fondo!",
            "¡Daños estructurales irreversibles en el blanco!",
            "¡El puente de mando enemigo ha colapsado!",
            "¡Confirmada detonación masiva en el pañol de municiones!",
            "¡La nave hostil no resistió nuestra andanada!",
            "¡Nave partida por el centro, se hunde rápido!",
            "¡Confirmamos hundimiento total del objetivo!",
            "¡Al fondo del océano, trabajo impecable de la batería!",
            "¡Nave enemiga deshabilitada y hundiéndose!",
            "¡Fuego cruzado exitoso, buque liquidado!",
            "¡Blanco partido por la quilla, se va a pique!",
            "¡Sonar confirma el colapso del casco en las profundidades!",
            "¡Esa salva selló el destino del barco enemigo!",
            "¡Explosión masiva confirmada, nave aniquilada!",
            "¡El casco enemigo ha cedido por completo!",
            "¡Contacto de radar extinguido, blanco neutralizado!",
            "¡Buen disparo artillería, objetivo en el fondo!",
            "¡El mar se traga los restos de la nave enemiga!",
            "¡Fuego a bordo incontrolable, el barco se sumerge!",
            "¡Impacto frontal certero, navío partido a la mitad!",
            "¡Punto de no retorno, el buque enemigo se hunde!",
            "¡Reporte del puesto de observación: blanco destruido!",
            "¡Andanada perfecta, sin capacidad de respuesta!",
            "¡La coraza de acero no soportó nuestros proyectiles!",
            "¡El buque hostil ya yace en el fondo marino!",
            "¡Destrucción total confirmada por los telémetros!",
            "¡Objetivo fuera de combate, navío a pique!",
            "¡Línea de flotación perforada, zozobra inmediata!",
            "¡El enemigo pierde otra unidad de combate!",
            "¡Impacto crítico en la línea central, colapso total!",
            "Adios, barquito",
            "¡Nave hostil destrozada por el fuego artillero!",
            "¡Vapor y llamas saliendo del casco enemigo!",
            "¡Confirmado por el puente: blanco aniquilado!",
            "¡Buque enemigo reducido a chatarra en el fondo!",
            "¡Directo al timón y a la santabárbara!",
            "¡Ese crucero ya no representa peligro alguno!",
            "¡Explosión secundaria en los depósitos de combustible!",
            "¡Al fondo con la flota enemiga!",
            "¡Fuego de precisión: impacto en zona vital!",
            "¡Salva mortal ejecutada con máxima eficacia!",
            "¡El casco del barco enemigo colapsó bajo la presión!",
            "¡Confirmación táctica: objetivo destruido y sumergido!",
            "¡Las calderas han reventado, se va a pique!",
            "¡Una baja más en la escuadra adversaria!",
            "¡El sonar registra el impacto del pecio en el lecho!",
            "¡Navío enemigo partido en dos mitades!",
            "¡Objetivo carbonizado y hundido por completo!",
            "¡Artillería pesada cumplió su misión al 100%!",
            "¡Impacto fulminante en el corazón del buque!",
            "¡Reporte de daños enemigos: pérdida total de la nave!",
            "¡El mar engulle a otra nave de la flota enemiga!",
            "¡Blanco pulverizado por nuestros cañones!",
            "¡Destrucción absoluta confirmada por el radar!",
            "¡Impacto demoledor en la santabárbara enemiga!",
            "¡El navío zozobra y desaparece de la superficie!",
            "¡Maniobra y disparo magistrales, nave hundida!",
            "¡La artillería enemiga ha quedado silenciada para siempre!",
            "¡Nave hostil perforada de proa a popa!",
            "¡Desintegración estructural bajo fuego concentrado!",
            "¡Ese barco ya es historia en este sector!",
            "¡A pique en cuestión de segundos, tiro perfecto!",
            "¡Reportando pecio sumergido en coordenadas fijadas!",
            "¡El fuego devora los restos de la nave antes de hundirse!",
            "¡Vía de agua incontrolable en el casco enemigo!",
            "¡Impacto directo en la sala de calderas!",
            "¡Victoria táctica en este cuadrante, navío hundido!",
            "¡La coraza enemiga saltó por los aires!",
            "¡Objetivo hundido sin posibilidad de rescate!",
            "¡Nave abatida por andanada concentrada!",
            "¡Ese cañonero no volverá a abrir fuego jamás!",
            "¡Puntería implacable, navío en el fondo del mar!",
            "¡La escuadra rival pierde otra pieza clave!",
            "¡Explosión cataclísmica en el puente enemigo!",
            "¡Los restos humeantes se sumergen en el abismo!",
            "¡Tocado, reventado y hundido sin titubeos!",
            "¡La batería naval acertó en el blanco crítico!",
            "¡Nave hostil eliminada del teatro de operaciones!",
            "¡Impacto devastador, hundimiento inmediato!",
            "¡Fuego en cubierta y colapso de mamparas!",
            "¡El mar recibe al buque enemigo como nuevo pecio!",
            "¡Disparo certero entre las dos torretas principales!",
            "¡Nave enemiga desmantelada por nuestra salva!",
            "¡Misión de interdicción cumplida, barco destruido!"
    };

    private static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(PHRASES));

    private PhraseBank() {
    }

    public static String randomPhrase(Random random) {
        return PHRASES[random.nextInt(PHRASES.length)];
    }

    public static int size() {
        return PHRASES.length;
    }

    public static List<String> all() {
        return ALL;
    }

    /**
     * Convierte una frase del repertorio al subconjunto de caracteres que
     * puede dibujar la fuente bitmap del HUD. Los emojis se sustituyen por
     * el icono de voz de la tarjeta de comunicaciones.
     */
    public static String toDisplayText(String phrase) {
        if (phrase == null) {
            return "";
        }

        StringBuilder result = new StringBuilder(phrase.length());
        boolean previousWasSpace = false;
        for (int i = 0; i < phrase.length(); i++) {
            char c = phrase.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                c = ' ';
            }
            if (c >= 32 && c <= 255) {
                if (c == ' ') {
                    if (!previousWasSpace) {
                        result.append(c);
                        previousWasSpace = true;
                    }
                } else {
                    result.append(c);
                    previousWasSpace = false;
                }
            }
        }
        return result.toString().trim();
    }
}
