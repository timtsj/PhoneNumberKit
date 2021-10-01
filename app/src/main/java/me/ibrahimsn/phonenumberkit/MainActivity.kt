package me.ibrahimsn.phonenumberkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import me.ibrahimsn.lib.PhoneNumberKit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val phoneNumberKit = PhoneNumberKit(this)
        val phoneNumberKit2 = PhoneNumberKit(this)

        // To attach an editTextLayout
        phoneNumberKit.attachToInput(textField, 971)
        phoneNumberKit2.attachToInput(editText, 971)

        // Setup country code picker optionally
        phoneNumberKit.setupCountryPicker(
            fragmentManager = supportFragmentManager,
            searchEnabled = true
        )

        // Provides example phone number for given country iso2 code
        val exampleNumber = phoneNumberKit.getExampleNumber("tr")

        // Parses raw phone number to phone object
        val parsedNumber = phoneNumberKit.parsePhoneNumber(
            number = "05066120000",
            defaultRegion = "us"
        )

        // Converts raw phone number to international formatted phone number
        // Ex: +90 506 606 00 00
        val formattedNumber = phoneNumberKit.formatPhoneNumber(
            number = "05066120000",
            defaultRegion = "tr"
        )

        // Provides country flag icon for given iso2 code
        val flag = phoneNumberKit.getFlagIcon("tr")

        // Provides country name, iso2 for given country code
        val country = phoneNumberKit.getCountry(90)
    }
}
