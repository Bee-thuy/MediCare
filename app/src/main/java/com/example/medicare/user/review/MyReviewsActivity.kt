package com.example.medicare.user.review

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.review.DoctorReviewRepository
import com.example.medicare.databinding.ActivityMyReviewsBinding

class MyReviewsActivity : AppCompatActivity() {
 private lateinit var binding: ActivityMyReviewsBinding
 override fun onCreate(state: Bundle?) { super.onCreate(state); binding=ActivityMyReviewsBinding.inflate(layoutInflater); setContentView(binding.root); binding.myReviewsBack.setOnClickListener { finish() } }
 override fun onResume() { super.onResume(); DoctorReviewRepository().getMine { result -> result.onSuccess { reviews -> binding.myReviewsList.removeAllViews(); binding.myReviewsEmpty.visibility=if(reviews.isEmpty()) View.VISIBLE else View.GONE; reviews.forEach { r -> val row=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundResource(R.drawable.manager_card_background); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=10} }; row.addView(TextView(this).apply{text="${r.doctorName} · ${"★".repeat(r.rating)}"; textSize=16f; setTextColor(getColor(R.color.manager_ink))}); row.addView(TextView(this).apply{text=r.comment.ifBlank{"Không có nhận xét."}; setPadding(0,6,0,0); setTextColor(getColor(R.color.manager_muted))}); binding.myReviewsList.addView(row) } } } }
}
