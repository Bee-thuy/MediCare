package com.example.medicare.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.R
import com.example.medicare.databinding.ItemOnboardingPageBinding

class OnboardingPagerAdapter(
    private val onPageAction: (position: Int) -> Unit,
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return PageViewHolder(
            ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position], position == pages.lastIndex) {
            onPageAction(position)
        }
    }

    override fun getItemCount() = pages.size

    class PageViewHolder(private val binding: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage, isLastPage: Boolean, onClick: () -> Unit) {
            binding.onboardingIllustration.setImageResource(page.illustration)
            binding.onboardingIndicator.setImageResource(page.indicator)
            binding.onboardingTitle.setText(page.title)
            binding.onboardingDescription.setText(page.description)
            binding.onboardingAction.setText(if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next)
            binding.onboardingAction.setOnClickListener { onClick() }
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
