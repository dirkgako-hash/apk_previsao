package com.dirosky.asianbets.domain.filters

import com.dirosky.asianbets.data.models.Bet365Data
import com.dirosky.asianbets.data.models.FilterResult
import kotlin.math.abs

/**
 * Filtro BeVIX-1 ∩ TNT-A
 * 
 * BeVIX-1: Linha de Over/Under fechou ACIMA de 2.75 E subiu (delta > 0)
 * TNT-A: Linha abertura ≥ 2.5, linha fecho ≥ 2.5, HC abertura ≥ 0.5, HC fecho ≥ 0.5
 * 
 * Delta (δ) = line_close − line_open
 * Níveis:
 *   A: δ > 0.50  (FORTE 🔥) - win=89% ROI=+0.51
 *   B: δ > 0.25  (BOM 🔵)   - win=85% ROI=+0.44
 *   C: δ > 0.00  (BASE 📊)  - win=80% ROI=+0.37
 */
object BeVixTntFilter {
    
    enum class Nivel(
        val label: String,
        val emoji: String,
        val winHist: Double,
        val roi: Double,
        val threshold: Double
    ) {
        A("FORTE", "🔥", 0.890, 0.513, 0.50),
        B("BOM", "🔵", 0.846, 0.438, 0.25),
        C("BASE", "📊", 0.804, 0.367, 0.00);
        
        companion object {
            fun fromDelta(delta: Double): Nivel {
                return when {
                    delta > A.threshold -> A
                    delta > B.threshold -> B
                    else -> C
                }
            }
        }
    }
    
    /**
     * Aplica filtro BeVIX-1 ∩ TNT-A aos dados da Bet365
     * 
     * @param bet365Data Dados JSON da Bet365
     * @return FilterResult se passou no filtro, null caso contrário
     */
    fun apply(bet365Data: Bet365Data): FilterResult? {
        // Extração dos valores
        val lineOpen = bet365Data.handicap1_3start?.over?.toDoubleOrNull() ?: return null
        val lineClose = bet365Data.handicap1_3end?.over?.toDoubleOrNull() ?: return null
        val hcOpen = bet365Data.handicap1_2start?.home?.toDoubleOrNull() ?: return null
        val hcClose = bet365Data.handicapEnd1_2?.home?.toDoubleOrNull() ?: return null
        val spread = bet365Data.spread1_2?.home?.toDoubleOrNull()
        
        // Delta
        val delta = lineClose - lineOpen
        
        // Handicaps absolutos
        val hcOpenAbs = abs(hcOpen)
        val hcCloseAbs = abs(hcClose)
        
        // BeVIX-1: linha fechou > 2.75 E subiu
        val beVix = lineClose > 2.75 && delta > 0
        
        // TNT-A: thresholds + delta positivo OU spread negativo
        val tntA = lineOpen >= 2.5 && 
                   lineClose >= 2.5 && 
                   hcOpenAbs >= 0.5 && 
                   hcCloseAbs >= 0.5 &&
                   (delta > 0 || (spread != null && spread < 0))
        
        // Se NÃO passou no filtro combinado, retorna null
        if (!beVix || !tntA) {
            return null
        }
        
        // Determinar nível
        val nivel = Nivel.fromDelta(delta)
        
        // Calcular probabilidade display
        val prob = calculateProbDisplay(delta, nivel)
        
        return FilterResult(
            delta = delta,
            lineClose = lineClose,
            hcOpen = hcOpen,
            nivel = nivel,
            prob = prob
        )
    }
    
    /**
     * Calcula probabilidade histórica ajustada pelo delta
     */
    private fun calculateProbDisplay(delta: Double, nivel: Nivel): Double {
        val baseWin = nivel.winHist
        
        val bonus = when (nivel) {
            Nivel.A -> {
                // Bonus até 3% para delta muito acima de 0.50
                minOf((delta - 0.50) * 0.03, 0.03)
            }
            Nivel.B -> {
                // Bonus proporcional entre B e A
                (delta - 0.25) / 0.25 * (Nivel.A.winHist - Nivel.B.winHist)
            }
            Nivel.C -> {
                // Bonus proporcional entre C e B
                if (delta > 0) {
                    (delta / 0.25) * (Nivel.B.winHist - Nivel.C.winHist)
                } else {
                    0.0
                }
            }
        }
        
        // Cap em 94%
        return minOf(baseWin + bonus, 0.94)
    }
    
    /**
     * Formata probabilidade para display (ex: "89%")
     */
    fun formatProb(prob: Double): String {
        return "${(prob * 100).toInt()}%"
    }
}
