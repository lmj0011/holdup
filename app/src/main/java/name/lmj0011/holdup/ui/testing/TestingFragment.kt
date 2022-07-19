package name.lmj0011.holdup.ui.testing

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import name.lmj0011.holdup.R
import name.lmj0011.holdup.databinding.FragmentTestingBinding
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface

class TestingFragment: Fragment(R.layout.fragment_testing), BaseFragmentInterface {

    private lateinit var binding: FragmentTestingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
    }

    /**
     *  These functions should be called in Fragment.onViewCreated
     *  in the following order
     */
    override fun setupBinding(view: View) {
        binding = FragmentTestingBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        val offlineTab = binding.pattonTabLayout.newTab()
        val onlineTab = binding.pattonTabLayout.newTab()

        offlineTab.text = "off"
        offlineTab.icon = getDrawable(requireContext(), R.drawable.ic_baseline_radio_button_unchecked_24)

        onlineTab.text = "on"
        onlineTab.icon = getDrawable(requireContext(), R.drawable.ic_baseline_radio_button_checked_24)

        binding.pattonTabLayout.addTab(offlineTab, 0)
        binding.pattonTabLayout.addTab(onlineTab, 1)
    }

    override fun setupObservers() {
        binding.pattonTabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            /**
             * Called when a tab enters the selected state.
             *
             * @param tab The tab that was selected
             */
            override fun onTabSelected(tab: TabLayout.Tab?) {
                toggleTabIcons(tab)
            }

            /**
             * Called when a tab exits the selected state.
             *
             * @param tab The tab that was unselected
             */
            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            /**
             * Called when a tab that is already selected is chosen again by the user. Some applications may
             * use this action to return to the top level of a category.
             *
             * @param tab The tab that was reselected.
             */
            override fun onTabReselected(tab: TabLayout.Tab?) {
                toggleTabIcons(tab)
            }

            fun toggleTabIcons(tab: TabLayout.Tab?) {
                tab?.let {
                    it.icon = getDrawable(requireContext(), R.drawable.ic_baseline_radio_button_checked_24)

                    when(it.position) {
                        0 -> {
                            binding.pattonTabLayout.getTabAt(1)?.let { ttab ->
                                ttab.icon = getDrawable(requireContext(), R.drawable.ic_baseline_radio_button_unchecked_24)
                            }
                        }
                        1 -> {
                            binding.pattonTabLayout.getTabAt(0)?.let { ttab ->
                                ttab.icon = getDrawable(requireContext(), R.drawable.ic_baseline_radio_button_unchecked_24)
                            }
                        }
                        else -> {}
                    }
                }
            }

        })
    }

    override fun setupRecyclerView() {}

    /**
     *  clear any User input data from this Fragment
     */
    override fun clearUserInputViews() {}
}