package com.example.medicare.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.example.medicare.R

class OnboardingPagerAdapter(
    private val onPageAction: (position: Int) -> Unit,
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position], position == pages.lastIndex) {
            onPageAction(position)
        }
    }

    override fun getItemCount() = pages.size

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val illustration: ImageView = view.findViewById(R.id.onboardingIllustration)
        private val indicator: ImageView = view.findViewById(R.id.onboardingIndicator)
        private val title: TextView = view.findViewById(R.id.onboardingTitle)
        private val description: TextView = view.findViewById(R.id.onboardingDescription)
        private val action: MaterialButton = view.findViewById(R.id.onboardingAction)

        fun bind(page: OnboardingPage, isLastPage: Boolean, onClick: () -> Unit) {
            illustration.setImageResource(page.illustration)
            indicator.setImageResource(page.indicator)
            title.setText(page.title)
            description.setText(page.description)
            action.setText(if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next)
            action.setOnClickListener { onClick() }
        }
    }

    data class OnboardingPage(
        @param:DrawableRes val illustration: Int,
        @param:DrawableRes val indicator: Int,
        @param:StringRes val title: Int,
        @param:StringRes val description: Int,
    )

    private companion object {
        val pages = listOf(
            OnboardingPage(
                R.drawable.onboarding_illustration_care,
                R.drawable.onboarding_indicator_1,
                R.string.onboarding_page_1_title,
                R.string.onboarding_page_1_description,
            ),
            OnboardingPage(
                R.drawable.onboarding_illustration_routine,
                R.drawable.onboarding_indicator_2,
                R.string.onboarding_page_2_title,
                R.string.onboarding_page_2_description,
            ),
            OnboardingPage(
                R.drawable.onboarding_illustration_ready,
                R.drawable.onboarding_indicator_3,
                R.string.onboarding_page_3_title,
                R.string.onboarding_page_3_description,
            ),
        )
    }
}
