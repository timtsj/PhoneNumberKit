package me.ibrahimsn.lib.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.ibrahimsn.lib.Countries
import me.ibrahimsn.lib.Country
import me.ibrahimsn.lib.R
import me.ibrahimsn.lib.util.showIf
import java.util.*

class CountryPickerBottomSheet : BottomSheetDialogFragment() {

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + Dispatchers.IO)
    private var itemAdapter: CountryAdapter? = null
    private val countries = Countries.list
    private var isSearchEnabled: Boolean = false
    var onCountrySelectedListener: ((Country?) -> Unit)? = null
    private val vInput get() = requireView().findViewById<SearchView>(R.id.searchView)
    private val vList get() = requireView().findViewById<RecyclerView>(R.id.recyclerView)
    private val vClose get() = requireView().findViewById<AppCompatImageView>(R.id.imageButtonClose)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_PhoneNumberKit_BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.bottom_sheet_country_picker,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vInput.showIf(isSearchEnabled)
        vList.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
            adapter = itemAdapter
        }
        vClose.setOnClickListener {
            dismiss()
        }
        vInput.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchCountries(newText)
                return true
            }
        })
    }

    fun setup(@LayoutRes itemLayout: Int, searchEnabled: Boolean = false) {
        itemAdapter = CountryAdapter(itemLayout).apply {
            setup(countries)
            isSearchEnabled = searchEnabled
            onItemClickListener = {
                onCountrySelectedListener?.invoke(it)
                dismiss()
            }
        }
    }

    private fun searchCountries(query: String?) {
        scope.launch {
            query?.let {
                val filtered = countries.filter {
                    it.countryCode.toString().startsWith(query) ||
                            it.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))
                }
                vList.post {
                    itemAdapter?.setup(filtered)
                }
            }
        }
    }

    companion object {
        fun newInstance() = CountryPickerBottomSheet()
        const val TAG = "tag-country-picker"
    }
}
