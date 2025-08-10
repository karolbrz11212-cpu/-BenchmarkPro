package com.example.benchmarkpro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2
    override fun createFragment(position: Int): Fragment = when(position) {
        0 -> InfoFragment()
        1 -> BenchmarkFragment()
        else -> InfoFragment()
    }
}
