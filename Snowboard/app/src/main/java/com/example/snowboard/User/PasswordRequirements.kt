package com.example.snowboard.User

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.snowboard.R
import com.example.snowboard.databinding.ViewPasswordRequirementsBinding
import com.example.snowboard.databinding.ViewPasswordRuleBinding

/**
 * Live checklist under a password field: red X for unmet rules, green check once met.
 * When every rule is met the checklist hides and [strongIcon] shows inside the field.
 */
object PasswordRequirements {

    fun attach(editText: EditText, binding: ViewPasswordRequirementsBinding, strongIcon: ImageView) {
        val rows = PasswordValidator.Rule.entries.associateWith { rule ->
            ViewPasswordRuleBinding.inflate(LayoutInflater.from(binding.root.context), binding.root, true)
                .also { it.ruleText.setText(rule.labelRes) }
        }

        fun refresh() {
            val password = editText.text.toString()
            val strong = PasswordValidator.validate(password) == null
            strongIcon.visibility = if (strong) View.VISIBLE else View.GONE
            binding.root.visibility =
                if (!strong && (editText.hasFocus() || password.isNotEmpty())) View.VISIBLE else View.GONE
            rows.forEach { (rule, row) ->
                val met = rule.isMet(password)
                val color = ContextCompat.getColor(
                    row.root.context,
                    if (met) R.color.password_rule_met else R.color.password_rule_unmet
                )
                row.ruleIcon.setImageResource(if (met) R.drawable.ic_check else R.drawable.ic_close)
                row.ruleIcon.imageTintList = ColorStateList.valueOf(color)
                row.ruleText.setTextColor(color)
            }
        }

        editText.doAfterTextChanged { refresh() }
        editText.setOnFocusChangeListener { _, _ -> refresh() }
        refresh()
    }

    /** Shows a green check in [icon] when [confirm] matches [password], a red X otherwise. */
    fun attachMatch(password: EditText, confirm: EditText, icon: ImageView) {
        fun refresh() {
            val confirmText = confirm.text.toString()
            if (confirmText.isEmpty()) {
                icon.visibility = View.GONE
                return
            }
            val match = confirmText == password.text.toString()
            icon.setImageResource(if (match) R.drawable.ic_check else R.drawable.ic_close)
            icon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    icon.context,
                    if (match) R.color.password_rule_met else R.color.password_rule_unmet
                )
            )
            icon.visibility = View.VISIBLE
        }

        password.doAfterTextChanged { refresh() }
        confirm.doAfterTextChanged { refresh() }
        refresh()
    }
}
