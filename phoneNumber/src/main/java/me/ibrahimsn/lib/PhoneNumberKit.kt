package me.ibrahimsn.lib

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.textfield.TextInputLayout
import me.ibrahimsn.lib.Constants.CHAR_DASH
import me.ibrahimsn.lib.Constants.CHAR_PLUS
import me.ibrahimsn.lib.Constants.CHAR_SPACE
import me.ibrahimsn.lib.Constants.KEY_DASH
import me.ibrahimsn.lib.Constants.KEY_DIGIT
import me.ibrahimsn.lib.Constants.KEY_SPACE
import me.ibrahimsn.lib.bottomsheet.CountryPickerBottomSheet
import me.ibrahimsn.lib.core.Core
import me.ibrahimsn.lib.util.*
import java.util.*

class PhoneNumberKit(private val context: Context) {

    private val core = Core(context)
    private var input: EditText? = null
    private var textInputLayout: TextInputLayout? = null
    private var country: Country? = null
    private var format: String = ""
    private var hasManualCountry = false
    private var rawInput: CharSequence?
        get() = input?.text
        set(value) {
            input?.tag = Constants.VIEW_TAG
            input?.clear()
            input?.append(value)
            input?.tag = null
        }
    val isValid: Boolean get() = validate(rawInput)
    private val textWatcher = object : PhoneNumberTextWatcher() {
        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
            if (input?.tag != Constants.VIEW_TAG) {
                val parsedNumber = core.parsePhoneNumber(
                    rawInput.toString().clearSpaces(),
                    country?.iso2
                )

                // Update country flag and mask if detected as a different one
                if (country == null || country?.countryCode != parsedNumber?.countryCode) {
                    if (!hasManualCountry) {
                        setCountry(getCountry(parsedNumber?.countryCode))
                    }
                }

                if (count != 0) {
                    applyFormat()
                }

                validate(rawInput)
            }
        }
    }

    private fun applyFormat() {
        rawInput?.let { raw ->
            // Clear all of the non-digit characters from the phone number
            val pureNumber = raw.filter { i -> i.isDigit() }.toMutableList()

            // Add plus to beginning of the number
            pureNumber.add(0, CHAR_PLUS)

            for (i in format.indices) {
                if (pureNumber.size > i) {
                    // Put required format spaces
                    if (format[i] == KEY_SPACE && pureNumber[i] != CHAR_SPACE) {
                        pureNumber.add(i, CHAR_SPACE)
                        continue
                    }

                    // Put required format dashes
                    if (format[i] == KEY_DASH && pureNumber[i] != CHAR_DASH) {
                        pureNumber.add(i, CHAR_DASH)
                        continue
                    }
                }
            }

            if (pureNumber.size > 1) {
                rawInput = pureNumber.toRawString()
            }
        }
    }

    private fun setCountry(country: Country?, isManual: Boolean = false, prefill: Boolean = false) {
        country?.let {
            this.country = country

            // Setup country icon
            getFlagIcon(country.iso2)?.let { icon ->
                textInputLayout?.startIconDrawable = icon
            }

            // Set text length limit according to the example phone number
            core.getExampleNumber(country.iso2)?.let { example ->
                if (isManual) {
                    hasManualCountry = true
                }
                if (isManual || prefill) {
                    rawInput = if (country.countryCode != example.countryCode) {
                        example.countryCode.prependPlus() + country.countryCode
                    } else {
                        country.countryCode.prependPlus()
                    }
                }
            }

            core.formatPhoneNumber(core.getExampleNumber(country.iso2))?.let { number ->
                input?.filters = arrayOf(InputFilter.LengthFilter(number.length))
                format = createNumberFormat(number)
                applyFormat()
            }
        }
    }

    fun updateCountry(countryIso2: String) {
        setCountry(
            country = getCountry(countryIso2.trim().toLowerCase(Locale.ENGLISH))
                ?: Countries.list[0],
            prefill = true
        )
    }

    // Creates a pattern like +90 506 555 55 55 -> +0010001000100100
    private fun createNumberFormat(number: String): String {
        var format = number.replace("(\\d)".toRegex(), KEY_DIGIT.toString())
        format = format.replace("(\\s)".toRegex(), KEY_SPACE.toString())
        return format
    }

    fun attachToInput(textInputLayout: TextInputLayout, defaultCountry: Int) {
        setupInput(textInputLayout.editText, textInputLayout, getCountry(defaultCountry))
    }

    fun attachToInput(textInputLayout: TextInputLayout, countryIso2: String) {
        setupInput(
            textInputLayout.editText,
            textInputLayout,
            getCountry(countryIso2.trim().toLowerCase(Locale.ENGLISH))
        )
    }

    fun attachToInput(input: EditText, defaultCountry: Int) {
        setupInput(input, null, getCountry(defaultCountry))
    }

    private fun setupInput(input: EditText?, textInputLayout: TextInputLayout?, country: Country?) {
        this.textInputLayout = textInputLayout
        this.input = input
        this.input?.inputType = InputType.TYPE_CLASS_PHONE
        this.input?.addTextChangedListener(textWatcher)

        this.textInputLayout?.isStartIconVisible = true
        this.textInputLayout?.isStartIconCheckable = true
        this.textInputLayout?.setStartIconTintList(null)

        // Set initial country
        setCountry(
            country = country ?: Countries.list[0],
            prefill = true
        )
    }

    /**
     * Sets up country code picker bottomSheet
     */
    fun setupCountryPicker(
        fragmentManager: FragmentManager,
        itemLayout: Int = R.layout.item_country_picker,
        searchEnabled: Boolean = false
    ) {
        textInputLayout?.setStartIconOnClickListener {
            showCountryPicker(fragmentManager, itemLayout, searchEnabled)
        }
    }

    fun showCountryPicker(
        fragmentManager: FragmentManager,
        itemLayout: Int = R.layout.item_country_picker,
        searchEnabled: Boolean = false
    ) {
        CountryPickerBottomSheet.newInstance().apply {
            setup(itemLayout, searchEnabled)
            onCountrySelectedListener = { country ->
                setCountry(country, true)
            }
            show(
                fragmentManager,
                CountryPickerBottomSheet.TAG
            )
        }
    }

    /**
     * Parses raw phone number into phone object
     */
    fun parsePhoneNumber(number: String?, defaultRegion: String?): Phone? {
        core.parsePhoneNumber(number, defaultRegion)?.let { phone ->
            return Phone(
                nationalNumber = phone.nationalNumber,
                countryCode = phone.countryCode,
                rawInput = phone.rawInput,
                numberOfLeadingZeros = phone.numberOfLeadingZeros
            )
        }
        return null
    }

    /**
     * Formats raw phone number into international phone
     */
    fun formatPhoneNumber(number: String?, defaultRegion: String?): String? {
        return core.formatPhoneNumber(core.parsePhoneNumber(number, defaultRegion))
    }

    /**
     * Provides an example phone number according to country iso2 code
     */
    fun getExampleNumber(iso2: String?): Phone? {
        core.getExampleNumber(iso2)?.let { phone ->
            return Phone(
                nationalNumber = phone.nationalNumber,
                countryCode = phone.countryCode,
                rawInput = phone.rawInput,
                numberOfLeadingZeros = phone.numberOfLeadingZeros
            )
        }
        return null
    }

    /**
     * Provides country flag icon for given country iso2 code
     */
    fun getFlagIcon(iso2: String?): Drawable? {
        return try {
            ContextCompat.getDrawable(
                context, context.resources.getIdentifier(
                    "country_flag_$iso2",
                    "drawable",
                    context.packageName
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Provides country for given country code
     */
    fun getCountry(countryCode: Int?): Country? {
        for (country in Countries.list) {
            if (country.countryCode == countryCode) {
                return country
            }
        }
        return null
    }

    /**
     * Provides country for given country iso2
     */
    fun getCountry(countryIso2: String?): Country? {
        for (country in Countries.list) {
            if (country.iso2 == countryIso2) {
                return country
            }
        }
        return null
    }

    private fun validate(number: CharSequence?): Boolean {
        if (number == null) return false
        return core.validateNumber(number.toString(), country?.iso2)
    }
}
