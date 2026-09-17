package com.gis.supermercados.core.common

import androidx.annotation.StringRes
import com.gis.supermercados.R
import java.util.regex.Pattern

/** Resultado de la validacion de un campo: valido, o invalido con mensaje localizable. */
data class ValidationResult(
    val isValid: Boolean,
    @StringRes val errorRes: Int? = null,
    val errorArg: String? = null,
) {
    val errorText: UiText?
        get() = errorRes?.let { id ->
            errorArg?.let { UiText.of(id, it) } ?: UiText.of(id)
        }

    companion object {
        val Valid = ValidationResult(true)
        fun invalid(@StringRes errorRes: Int, arg: String? = null) =
            ValidationResult(false, errorRes, arg)
    }
}

/**
 * Validaciones robustas reutilizables por todos los formularios de la app.
 * Centralizarlas garantiza reglas consistentes en telefono y tablet.
 */
object Validators {

    private val EMAIL_PATTERN: Pattern =
        Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    private val SKU_PATTERN: Pattern = Pattern.compile("^[A-Za-z0-9._\\-]{3,32}$")
    private val BARCODE_PATTERN: Pattern = Pattern.compile("^[A-Za-z0-9\\-]{6,24}$")
    private val USERNAME_PATTERN: Pattern = Pattern.compile("^[A-Za-z0-9._]{3,24}$")
    private val PHONE_PATTERN: Pattern = Pattern.compile("^[0-9+()\\-\\s]{6,20}$")
    private val TIME_PATTERN: Pattern = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$")

    private const val MAX_AMOUNT_CENTS = 9_999_999_999_99L

    fun required(value: CharSequence?): ValidationResult =
        if (value.isNullOrBlank()) {
            ValidationResult.invalid(R.string.error_field_required)
        } else {
            ValidationResult.Valid
        }

    fun minLength(value: CharSequence?, min: Int): ValidationResult =
        if ((value?.length ?: 0) < min) {
            ValidationResult.invalid(R.string.error_min_length, min.toString())
        } else {
            ValidationResult.Valid
        }

    fun maxLength(value: CharSequence?, max: Int): ValidationResult =
        if ((value?.length ?: 0) > max) {
            ValidationResult.invalid(R.string.error_max_length, max.toString())
        } else {
            ValidationResult.Valid
        }

    /** Texto libre obligatorio: no vacio, dentro de [min] y [max] caracteres. */
    fun text(value: CharSequence?, min: Int = 2, max: Int = 120): ValidationResult {
        required(value).let { if (!it.isValid) return it }
        minLength(value, min).let { if (!it.isValid) return it }
        return maxLength(value, max)
    }

    /** Texto libre opcional: si viene vacio es valido, si no, se valida longitud. */
    fun optionalText(value: CharSequence?, max: Int = 200): ValidationResult {
        if (value.isNullOrBlank()) return ValidationResult.Valid
        return maxLength(value, max)
    }

    fun email(value: CharSequence?, optional: Boolean = false): ValidationResult {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return if (optional) ValidationResult.Valid else required(text)
        return if (EMAIL_PATTERN.matcher(text).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.invalid(R.string.error_invalid_email)
        }
    }

    fun phone(value: CharSequence?, optional: Boolean = true): ValidationResult {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return if (optional) ValidationResult.Valid else required(text)
        return if (PHONE_PATTERN.matcher(text).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.invalid(R.string.error_invalid_phone)
        }
    }

    fun username(value: CharSequence?): ValidationResult {
        required(value).let { if (!it.isValid) return it }
        minLength(value, 3).let { if (!it.isValid) return it }
        return if (USERNAME_PATTERN.matcher(value.toString().trim()).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.invalid(R.string.error_invalid_username)
        }
    }

    /**
     * Contrasena segura: minimo 8 caracteres, al menos una letra y un digito.
     * Se aplica en el alta de usuario y en el cambio de contrasena.
     */
    fun password(value: CharSequence?): ValidationResult {
        val text = value.orEmpty()
        if (text.length < AppConstants.PASSWORD_MIN_LENGTH) {
            return ValidationResult.invalid(
                R.string.error_password_min_length,
                AppConstants.PASSWORD_MIN_LENGTH.toString()
            )
        }
        val hasLetter = text.any { it.isLetter() }
        val hasDigit = text.any { it.isDigit() }
        return if (hasLetter && hasDigit) {
            ValidationResult.Valid
        } else {
            ValidationResult.invalid(R.string.error_password_weak)
        }
    }

    fun passwordsMatch(password: CharSequence?, confirmation: CharSequence?): ValidationResult =
        if (password.isNullOrEmpty() || password.toString() != confirmation.toString()) {
            ValidationResult.invalid(R.string.error_passwords_dont_match)
        } else {
            ValidationResult.Valid
        }

    fun sku(value: CharSequence?): ValidationResult {
        required(value).let { if (!it.isValid) return it }
        minLength(value, 3).let { if (!it.isValid) return it }
        return if (SKU_PATTERN.matcher(value.toString().trim()).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.invalid(R.string.error_invalid_sku)
        }
    }

    fun barcode(value: CharSequence?, optional: Boolean = true): ValidationResult {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return if (optional) ValidationResult.Valid else required(text)
        return if (BARCODE_PATTERN.matcher(text).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.invalid(R.string.error_invalid_barcode)
        }
    }

    /** Importe monetario valido. Acepta formato con punto o coma decimal. */
    fun money(
        value: CharSequence?,
        allowZero: Boolean = true,
        maxCents: Long = MAX_AMOUNT_CENTS,
    ): ValidationResult {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return ValidationResult.invalid(R.string.error_field_required)
        val cents = Money.parse(text)
            ?: return ValidationResult.invalid(R.string.error_invalid_amount)
        if (cents < 0L || (!allowZero && cents == 0L)) {
            return ValidationResult.invalid(R.string.error_amount_must_be_positive)
        }
        if (cents > maxCents) return ValidationResult.invalid(R.string.error_amount_too_large)
        return ValidationResult.Valid
    }

    fun positiveInt(
        value: CharSequence?,
        max: Int = 1_000_000,
        allowZero: Boolean = true,
    ): ValidationResult {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return ValidationResult.invalid(R.string.error_field_required)
        val number = text.toIntOrNull()
            ?: return ValidationResult.invalid(R.string.error_invalid_number)
        if (number < 0 || (!allowZero && number == 0) || number > max) {
            return ValidationResult.invalid(R.string.error_invalid_number)
        }
        return ValidationResult.Valid
    }

    fun percent(value: CharSequence?, max: Int = 100): ValidationResult {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return ValidationResult.Valid
        val number = text.replace(',', '.').toDoubleOrNull()
            ?: return ValidationResult.invalid(R.string.error_invalid_number)
        if (number < 0.0 || number > max) {
            return ValidationResult.invalid(R.string.error_invalid_percent, max.toString())
        }
        return ValidationResult.Valid
    }

    fun timeOfDay(value: CharSequence?): ValidationResult {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return ValidationResult.invalid(R.string.error_field_required)
        return if (TIME_PATTERN.matcher(text).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.invalid(R.string.error_invalid_time)
        }
    }

    /** Rango de fechas coherente: el fin no puede ser anterior al inicio. */
    fun dateRange(startMillis: Long, endMillis: Long): ValidationResult =
        if (endMillis < startMillis) {
            ValidationResult.invalid(R.string.error_invalid_date_range)
        } else {
            ValidationResult.Valid
        }

    /** Devuelve el primer error encontrado, o [ValidationResult.Valid]. */
    fun firstError(vararg results: ValidationResult): ValidationResult =
        results.firstOrNull { !it.isValid } ?: ValidationResult.Valid
}
