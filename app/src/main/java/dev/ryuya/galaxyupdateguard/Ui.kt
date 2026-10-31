package dev.ryuya.galaxyupdateguard

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import kotlin.jvm.JvmStatic
import kotlin.math.round

object Ui {
    @JvmStatic
    fun dp(context: Context, value: Int): Int {
        return Math.round(value * context.getResources().getDisplayMetrics().density)
    }

    @JvmStatic
    fun column(context: Context, padding: Int): LinearLayout {
        val view = LinearLayout(context)
        view.setOrientation(LinearLayout.VERTICAL)
        val p = Ui.dp(context, padding)
        view.setPadding(p, p, p, p)
        return view
    }

    @JvmStatic
    fun color(view: View?, token: Int): Int {
        return MaterialColors.getColor(view!!, token)
    }

    @JvmStatic
    fun card(parent: LinearLayout, color: Int): LinearLayout {
        val context = parent.getContext()
        val card = MaterialCardView(context)
        card.setRadius(Ui.dp(context, 28).toFloat())
        card.setStrokeWidth(0)
        card.setCardBackgroundColor(Ui.color(card, color))
        val params = LayoutParams(-1, -2)
        params.bottomMargin = Ui.dp(context, 16)
        parent.addView(card, params)
        val body = Ui.column(context, 24)
        card.addView(body)
        return body
    }

    @JvmStatic
    fun text(parent: LinearLayout, text: CharSequence?, style: Int): MaterialTextView {
        val label = MaterialTextView(parent.getContext())
        label.setTextAppearance(style)
        label.setText(text)
        label.setLineSpacing(Ui.dp(parent.getContext(), 2).toFloat(), 1f)
        val params = LayoutParams(-1, -2)
        params.topMargin = Ui.dp(parent.getContext(), 6)
        params.bottomMargin = Ui.dp(parent.getContext(), 6)
        parent.addView(label, params)
        return label
    }
}
