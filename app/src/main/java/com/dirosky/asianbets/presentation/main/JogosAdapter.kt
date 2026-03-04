package com.dirosky.asianbets.presentation.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dirosky.asianbets.R
import com.dirosky.asianbets.data.models.JogoEntity
import com.dirosky.asianbets.domain.filters.BeVixTntFilter

class JogosAdapter(
    private var jogos: List<JogoEntity> = emptyList()
) : RecyclerView.Adapter<JogosAdapter.JogoViewHolder>() {

    fun updateJogos(newJogos: List<JogoEntity>) {
        jogos = newJogos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JogoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jogo, parent, false)
        return JogoViewHolder(view)
    }

    override fun onBindViewHolder(holder: JogoViewHolder, position: Int) {
        holder.bind(jogos[position])
    }

    override fun getItemCount() = jogos.size

    class JogoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val txtNivel: TextView = itemView.findViewById(R.id.txtNivel)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        private val txtHome: TextView = itemView.findViewById(R.id.txtHome)
        private val txtAway: TextView = itemView.findViewById(R.id.txtAway)
        private val txtLeague: TextView = itemView.findViewById(R.id.txtLeague)
        private val txtTime: TextView = itemView.findViewById(R.id.txtTime)
        private val txtDelta: TextView = itemView.findViewById(R.id.txtDelta)
        private val txtLine: TextView = itemView.findViewById(R.id.txtLine)
        private val txtProb: TextView = itemView.findViewById(R.id.txtProb)
        private val layoutScore: View = itemView.findViewById(R.id.layoutScore)
        private val txtMinuto: TextView = itemView.findViewById(R.id.txtMinuto)
        private val txtScore: TextView = itemView.findViewById(R.id.txtScore)
        private val layoutAlerta: View = itemView.findViewById(R.id.layoutAlerta)
        private val txtOdd: TextView = itemView.findViewById(R.id.txtOdd)

        fun bind(jogo: JogoEntity) {
            // Nível
            val nivelInfo = BeVixTntFilter.Nivel.valueOf(jogo.nivel)
            txtNivel.text = "${nivelInfo.emoji} ${nivelInfo.label}"
            txtNivel.setBackgroundColor(when (jogo.nivel) {
                "A" -> Color.parseColor("#FFF3E0") // Laranja claro
                "B" -> Color.parseColor("#E3F2FD") // Azul claro
                else -> Color.parseColor("#F5F5F5") // Cinza claro
            })
            txtNivel.setTextColor(when (jogo.nivel) {
                "A" -> Color.parseColor("#FF6F00")
                "B" -> Color.parseColor("#1976D2")
                else -> Color.parseColor("#616161")
            })

            // Status
            txtStatus.text = when (jogo.status.lowercase()) {
                "live" -> "🔴 AO VIVO"
                "ht", "halftime" -> "⏸️ INTERVALO"
                "ended" -> if (jogo.alertaLive) "✅✅✅ GANHOU" else "❌ PERDEU"
                else -> "⏳ Aguarda"
            }
            txtStatus.setTextColor(when (jogo.status.lowercase()) {
                "live" -> Color.parseColor("#D32F2F")
                "ht", "halftime" -> Color.parseColor("#FF6F00")
                "ended" -> if (jogo.alertaLive) Color.parseColor("#2E7D32") else Color.parseColor("#757575")
                else -> Color.parseColor("#616161")
            })

            // Times
            txtHome.text = jogo.home
            txtAway.text = jogo.away

            // Liga e horário
            txtLeague.text = "🏆 ${jogo.league}"
            txtTime.text = "🕐 ${jogo.eventTime ?: "—:—"}"

            // Métricas
            txtDelta.text = String.format("%+.3f", jogo.delta)
            txtDelta.setTextColor(
                if (jogo.delta > 0.5) Color.parseColor("#2E7D32")
                else if (jogo.delta > 0.25) Color.parseColor("#558B2F")
                else Color.parseColor("#7CB342")
            )
            txtLine.text = String.format("%.2f", jogo.lineClose)
            txtProb.text = jogo.prob

            // Placar (se live ou HT ou ended)
            if (jogo.status.lowercase() in listOf("live", "ht", "halftime", "ended")) {
                layoutScore.visibility = View.VISIBLE
                
                if (jogo.minutoLive > 0) {
                    txtMinuto.text = "⏱️ ${jogo.minutoLive}'"
                    txtMinuto.visibility = View.VISIBLE
                } else {
                    txtMinuto.visibility = View.GONE
                }
                
                val scoreText = when {
                    jogo.status.lowercase() == "ended" && jogo.ftScore.isNotEmpty() -> 
                        "🏁 ${jogo.ftScore}"
                    jogo.status.lowercase() in listOf("ht", "halftime") && jogo.htScore.isNotEmpty() -> 
                        "📊 HT: ${jogo.htScore}"
                    jogo.scoreLive.isNotEmpty() -> 
                        "📊 ${jogo.scoreLive}"
                    else -> ""
                }
                
                if (scoreText.isNotEmpty()) {
                    txtScore.text = scoreText
                    txtScore.visibility = View.VISIBLE
                } else {
                    txtScore.visibility = View.GONE
                }
            } else {
                layoutScore.visibility = View.GONE
            }

            // Alerta de aposta
            if (jogo.alertaLive) {
                layoutAlerta.visibility = View.VISIBLE
                jogo.oddLive?.let {
                    txtOdd.text = "💰 ${String.format("%.2f", it)}"
                }
            } else {
                layoutAlerta.visibility = View.GONE
            }
        }
    }
}
