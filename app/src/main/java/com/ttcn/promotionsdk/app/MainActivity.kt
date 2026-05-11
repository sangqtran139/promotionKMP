package com.ttcn.promotionsdk.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.ttcn.promotionsdk.ui.utils.extension.hideSoftInput
import com.ttcn.promotionsdk.ui.utils.view.PRMSearchField
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(com.ttcn.promotionsdk.R.layout.fragment_my_endow)

        val recyclerView =
            findViewById<RecyclerView>(com.ttcn.promotionsdk.R.id.rcvVoucher)

        val editText =
            findViewById<PRMSearchField>(com.ttcn.promotionsdk.R.id.edtVoucher)

        val itemAdapter = ItemAdapter<GenericItem>()

        val fastAdapter = FastAdapter.with(itemAdapter)

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter = fastAdapter

        itemAdapter.add(
            listOf(
                TextItem("Ưu đãi của tôi"),
                PromotionItem(),
                PromotionItem(),
                PromotionItem(),
                PromotionItem(),
                PromotionItem(),
                PromotionItem(),
                TextItem("Ưu đãi khác"),
                PromotionItem(),
                PromotionItem(),
                PromotionItem(),
            )
        )


        editText.setOnSearchActionListener {
            hideSoftInput()
        }
    }
}
