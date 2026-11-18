package com.batallanaval.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PhraseBankTest {

    @Test
    public void testRepertoireHasMoreThanOneHundredUniquePhrases() {
        List<String> phrases = PhraseBank.all();

        assertTrue(phrases.size() > 100, "El repertorio debe superar las 100 frases");
        assertEquals(phrases.size(), new HashSet<>(phrases).size(), "Las frases deben ser unicas");

        for (String phrase : phrases) {
            assertFalse(phrase.isBlank(), "No debe haber frases vacias");
        }
    }

    @Test
    public void testDisplayTextKeepsSpanishCharactersAndStripsEmoji() {
        assertEquals("Aparentemente", PhraseBank.toDisplayText("Aparentemente \uD83D\uDDE3\uFE0F"));
        assertEquals("¿Que paso? ¡Nada!", PhraseBank.toDisplayText("¿Que paso? ¡Nada!"));
        assertEquals("Adios, barquito", PhraseBank.toDisplayText("  Adios,   barquito  "));
    }

    @Test
    public void testRandomPhraseComesFromRepertoire() {
        Random random = new Random(7);
        Set<String> phrases = new HashSet<>(PhraseBank.all());

        for (int i = 0; i < 100; i++) {
            assertTrue(phrases.contains(PhraseBank.randomPhrase(random)));
        }
    }
}
