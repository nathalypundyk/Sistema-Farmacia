package sistema_farmacia.demo.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimilitudService {

    /**
     * Calcula distancia de Levenshtein normalizada (0-100)
     * Mide el número de ediciones (inserción, eliminación, sustitución) necesarias
     */
    public int levenshtein(String a, String b) {
        if (a.equals(b)) return 100;
        if (a.isEmpty() || b.isEmpty()) return 0;

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + costo);
            }
        }

        int distancia = dp[a.length()][b.length()];
        int maxLen = Math.max(a.length(), b.length());
        return (int) (100 * (1 - (double) distancia / maxLen));
    }

    /**
     * Algoritmo Jaro-Winkler: excelente para nombres farmacéuticos
     * Penaliza diferencias al principio de la cadena
     */
    public int jaroWinkler(String s1, String s2) {
        if (s1.equals(s2)) return 100;

        double jaro = jaro(s1, s2);

        int prefixLen = 0;
        for (int i = 0; i < Math.min(s1.length(), s2.length()) && i < 4; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLen++;
            } else {
                break;
            }
        }

        double jaroWinkler = jaro + (prefixLen * 0.1 * (1 - jaro));
        return (int) (jaroWinkler * 100);
    }

    private double jaro(String s1, String s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int matchDistance = Math.max(s1.length(), s2.length()) / 2 - 1;
        matchDistance = Math.max(0, matchDistance);

        boolean[] s1Matches = new boolean[s1.length()];
        boolean[] s2Matches = new boolean[s2.length()];

        int matches = 0;
        int transpositions = 0;

        for (int i = 0; i < s1.length(); i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, s2.length());

            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) return 0.0;

        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }

        return (matches / (double) s1.length() +
                matches / (double) s2.length() +
                (matches - transpositions / 2.0) / matches) / 3.0;
    }

    /**
     * Similitud por nombre + concentración con ponderación
     * Las primeras palabras pesan más
     */
    public int similitudNombreConc(String nombre1, String conc1, String nombre2, String conc2) {
        // Similitud de nombre (peso 70%)
        int simNombre = Math.max(jaroWinkler(nombre1, nombre2), levenshtein(nombre1, nombre2));

        // Similitud de concentración (peso 30%)
        int simConc = 100;
        if (!conc1.isEmpty() && !conc2.isEmpty()) {
            simConc = Math.max(jaroWinkler(conc1, conc2), levenshtein(conc1, conc2));
        } else if (conc1.isEmpty() && conc2.isEmpty()) {
            simConc = 100;
        } else {
            simConc = 0;
        }

        return (int) (simNombre * 0.7 + simConc * 0.3);
    }

    /**
     * Similitud por palabras con ponderación de posición
     * Las primeras palabras pesan más que las últimas
     */
    public int similitudPorPalabras(String a, String b) {
        if (a.equals(b)) return 100;

        String[] pa = a.split("\\s+");
        String[] pb = b.split("\\s+");

        double puntaje = 0;
        int comparaciones = 0;

        int maxLen = Math.max(pa.length, pb.length);
        for (int i = 0; i < maxLen; i++) {
            String pa_i = i < pa.length ? pa[i] : "";
            String pb_i = i < pb.length ? pb[i] : "";

            if (pa_i.isEmpty() || pb_i.isEmpty()) {
                if (!pa_i.isEmpty() || !pb_i.isEmpty()) {
                    comparaciones++;
                }
                continue;
            }

            // Peso decreciente según posición
            double peso = 1.0 - (i * 0.1);
            peso = Math.max(0.1, peso);

            int sim = jaroWinkler(pa_i, pb_i);
            puntaje += sim * peso;
            comparaciones++;
        }

        if (comparaciones == 0) return 0;
        return (int) (puntaje / comparaciones);
    }

    /**
     * Similitud global: combina múltiples estrategias
     * Retorna el MÁXIMO entre todas las estrategias
     */
    public int similitudGlobal(String nombre1, String conc1, String nombre2, String conc2) {
        int jw = similitudNombreConc(nombre1, conc1, nombre2, conc2);
        int lev = similitudNombreConc(nombre1, conc1, nombre2, conc2);
        int palabras = similitudPorPalabras(nombre1 + " " + conc1, nombre2 + " " + conc2);

        return Math.max(jw, Math.max(lev, palabras));
    }

    /**
     * Detecta si es probable que sean duplicados (mismo medicamento pero nombres diferentes)
     */
    public boolean esProbalePosibleDuplicado(String nombre1, String conc1, String nombre2, String conc2) {
        int similitud = similitudGlobal(nombre1, conc1, nombre2, conc2);
        return similitud >= 75;
    }
}
