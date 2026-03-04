package com.dirosky.asianbets.presentation.main

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dirosky.asianbets.R
import com.dirosky.asianbets.data.models.JogoEntity

class JogosAdapter(
    private var jogos: List<JogoEntity> = emptyList()
) : RecyclerView.Adapter<JogosAdapter.JogoViewHolder>() {

    companion object {
        private const val TAG = "JogosAdapter"
    }

    fun updateJogos(newJogos: List<JogoEntity>) {
        jogos = newJogos
        notifyDataSetChanged()
        Log.d(TAG, "Updated with ${newJogos.size} games")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JogoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jogo, parent, false)
        return JogoViewHolder(view)
    }

    override fun onBindViewHolder(holder: JogoViewHolder, position: Int) {
        try {
            holder.bind(jogos[position])
        } catch (e: Exception) {
            Log.e(TAG, "Error binding position $position", e)
        }
    }

    override fun getItemCount() = jogos.size

    class JogoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val txtNivel: TextView? = itemView.findViewById(R.id.txtNivel)
        private val txtStatus: TextView? = itemView.findViewById(R.id.txtStatus)
        private val txtHome: TextView? = itemView.findViewById(R.id.txtHome)
        private val txtAway: TextView? = itemView.findViewById(R.id.txtAway)
        private val txtLeague: TextView? = itemView.findViewById(R.id.txtLeague)
        private val txtTime: TextView? = itemView.findViewById(R.id.txtTime)
        private val txtDelta: TextView? = itemView.findViewById(R.id.txtDelta)
        private val txtLine: TextView? = itemView.findViewById(R.id.txtLine)
        private val txtProb: TextView? = itemView.findViewById(R.id.txtProb)
        private val layoutScore: View? = itemView.findViewById(R.id.layoutScore)
        private val txtMinuto: TextView? = itemView.findViewById(R.id.txtMinuto)
        private val txtScore: TextView? = itemView.findViewById(R.id.txtScore)
        private val layoutAlerta: View? = itemView.findViewById(R.id.layoutAlerta)
        private val txtOdd: TextView? = itemView.findViewById(R.id.txtOdd)

        fun bind(jogo: JogoEntity) {
            try {
                // Nível
                val nivelText = when (jogo.nivel) {
                    "A" -> "🔥 FORTE"
                    "B" -> "🔵 BOM"
                    else -> "📊 BASE"
                }
                txtNivel?.text = nivelText
                txtNivel?.setBackgroundColor(when (jogo.nivel) {
                    "A" -> Color.parseColor("#FFF3E0")
                    "B" -> Color.parseColor("#E3F2FD")
                    else -> Color.parseColor("#F5F5F5")
                })
                txtNivel?.setTextColor(when (jogo.nivel) {
                    "A" -> Color.parseColor("#FF6F00")
                    "B" -> Color.parseColor("#1976D2")
                    else -> Color.parseColor("#616161")
                })

                // Status
                val statusText = when (jogo.status.lowercase()) {
                    "live" -> "🔴 AO VIVO"
                    "ht", "halftime" -> "⏸️ INTERVALO"
                    "ended" -> if (jogo.alertaLive) "✅ GANHOU" else "❌ PERDEU"
                    else -> "⏳ Aguarda"
                }
                txtStatus?.text = statusText

                // Times
                txtHome?.text = jogo.home
                txtAway?.text = jogo.away

                // Liga e horário
                txtLeague?.text = "🏆 ${jogo.league}"
                txtTime?.text = "🕐 ${jogo.eventTime ?: "—"}"

                // Métricas
                txtDelta?.text = String.format("%+.3f", jogo.delta)
                txtLine?.text = String.format("%.2f", jogo.lineClose)
                txtProb?.text = jogo.prob

                // Placar
                val isLiveOrEnded = jogo.status.lowercase() in listOf("live", "ht", "halftime", "ended")
                layoutScore?.visibility = if (isLiveOrEnded) View.VISIBLE else View.GONE
                
                if (isLiveOrEnded && jogo.minutoLive > 0) {
                    txtMinuto?.text = "⏱️ ${jogo.minutoLive}'"
                    txtMinuto?.visibility = View.VISIBLE
                } else {
                    txtMinuto?.visibility = View.GONE
                }
                
                if (isLiveOrEnded && jogo.scoreLive.isNotEmpty()) {
                    txtScore?.text = "📊 ${jogo.scoreLive}"
                    txtScore?.visibility = View.VISIBLE
                } else {
                    txtScore?.visibility = View.GONE
                }

                // Alerta
                layoutAlerta?.visibility = if (jogo.alertaLive) View.VISIBLE else View.GONE
                jogo.oddLive?.let {
                    txtOdd?.text = "💰 ${String.format("%.2f", it)}"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in bind", e)
            }
        }
    }
}
