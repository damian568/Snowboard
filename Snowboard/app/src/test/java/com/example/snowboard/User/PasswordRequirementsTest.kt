package com.example.snowboard.User

import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.snowboard.R
import com.example.snowboard.databinding.ViewPasswordRequirementsBinding
import com.example.snowboard.themedContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PasswordRequirementsTest {

    private lateinit var password: EditText
    private lateinit var confirm: EditText
    private lateinit var strongIcon: ImageView
    private lateinit var matchIcon: ImageView
    private lateinit var checklist: ViewPasswordRequirementsBinding

    @Before
    fun setUp() {
        val context = themedContext()
        password = EditText(context)
        confirm = EditText(context)
        strongIcon = ImageView(context)
        matchIcon = ImageView(context)
        checklist = ViewPasswordRequirementsBinding.inflate(LayoutInflater.from(context))
    }

    private fun rows(): List<LinearLayout> =
        (0 until (checklist.root as android.view.ViewGroup).childCount).map {
            (checklist.root as android.view.ViewGroup).getChildAt(it) as LinearLayout
        }

    private fun textOf(row: LinearLayout) = (row.findViewById<TextView>(R.id.rule_text)).text.toString()
    private fun iconRes(row: LinearLayout) = shadowOf(row.findViewById<ImageView>(R.id.rule_icon).drawable).createdFromResId

    // ---- attach ----

    @Test
    fun attach_addsOneRowPerRule_withItsLabel() {
        PasswordRequirements.attach(password, checklist, strongIcon)

        val labels = rows().map { textOf(it) }
        assertEquals(PasswordValidator.Rule.entries.size, labels.size)
        PasswordValidator.Rule.entries.forEachIndexed { i, rule ->
            assertEquals(password.context.getString(rule.labelRes), labels[i])
        }
    }

    @Test
    fun attach_emptyUnfocusedField_hidesEverything() {
        PasswordRequirements.attach(password, checklist, strongIcon)

        assertEquals(View.GONE, checklist.root.visibility)
        assertEquals(View.GONE, strongIcon.visibility)
    }

    @Test
    fun attach_typingAWeakPassword_showsTheChecklist_withMetAndUnmetRules() {
        PasswordRequirements.attach(password, checklist, strongIcon)

        password.setText("abcdefgh")

        assertEquals(View.VISIBLE, checklist.root.visibility)
        assertEquals(View.GONE, strongIcon.visibility)
        val icons = rows().map { iconRes(it) }
        // LENGTH, LOWERCASE met; UPPERCASE, NUMBER, SYMBOL not
        assertEquals(R.drawable.ic_check, icons[0])
        assertEquals(R.drawable.ic_close, icons[1])
        assertEquals(R.drawable.ic_check, icons[2])
        assertEquals(R.drawable.ic_close, icons[3])
        assertEquals(R.drawable.ic_close, icons[4])
    }

    @Test
    fun attach_aStrongPassword_hidesTheChecklist_andShowsTheStrongIcon() {
        PasswordRequirements.attach(password, checklist, strongIcon)

        password.setText("Str0ng!Pass")

        assertEquals(View.GONE, checklist.root.visibility)
        assertEquals(View.VISIBLE, strongIcon.visibility)
        rows().forEach { assertEquals(R.drawable.ic_check, iconRes(it)) }
    }

    @Test
    fun attach_goingBackToWeak_bringsTheChecklistBack() {
        PasswordRequirements.attach(password, checklist, strongIcon)
        password.setText("Str0ng!Pass")

        password.setText("Str0ng")

        assertEquals(View.VISIBLE, checklist.root.visibility)
        assertEquals(View.GONE, strongIcon.visibility)
    }

    // ---- attachMatch ----

    @Test
    fun attachMatch_emptyConfirmation_hidesTheIcon() {
        PasswordRequirements.attachMatch(password, confirm, matchIcon)

        assertEquals(View.GONE, matchIcon.visibility)
    }

    @Test
    fun attachMatch_matchingPasswords_showACheck() {
        PasswordRequirements.attachMatch(password, confirm, matchIcon)

        password.setText("Secret1!")
        confirm.setText("Secret1!")

        assertEquals(View.VISIBLE, matchIcon.visibility)
        assertEquals(R.drawable.ic_check, shadowOf(matchIcon.drawable).createdFromResId)
    }

    @Test
    fun attachMatch_differentPasswords_showACross() {
        PasswordRequirements.attachMatch(password, confirm, matchIcon)

        password.setText("Secret1!")
        confirm.setText("Secret2!")

        assertEquals(View.VISIBLE, matchIcon.visibility)
        assertEquals(R.drawable.ic_close, shadowOf(matchIcon.drawable).createdFromResId)
    }

    @Test
    fun attachMatch_changingThePasswordAfterwards_updatesTheIcon() {
        PasswordRequirements.attachMatch(password, confirm, matchIcon)
        password.setText("Secret1!")
        confirm.setText("Secret1!")

        password.setText("Changed1!")

        assertEquals(R.drawable.ic_close, shadowOf(matchIcon.drawable).createdFromResId)
    }

    @Test
    fun attachMatch_clearingTheConfirmation_hidesTheIconAgain() {
        PasswordRequirements.attachMatch(password, confirm, matchIcon)
        confirm.setText("x")

        confirm.setText("")

        assertEquals(View.GONE, matchIcon.visibility)
    }
}
