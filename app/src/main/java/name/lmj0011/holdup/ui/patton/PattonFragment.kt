package name.lmj0011.holdup.ui.patton

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.databinding.FragmentPattonBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.util.*
import name.lmj0011.holdup.ui.submission.bottomsheet.BottomSheetAccountsFragment
import org.json.JSONObject
import org.kodein.di.instance
import org.kodein.di.subDI
import timber.log.Timber

class PattonFragment: Fragment(R.layout.fragment_patton), BaseFragmentInterface {

    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var viewModel: PattonViewModel
    private lateinit var binding: FragmentPattonBinding

    private lateinit var listAdapter: ArrayAdapter<String>
    lateinit var bottomSheetAccountsFragment: BottomSheetAccountsFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = PattonViewModel.getNewInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        dataStoreHelper = (requireContext().applicationContext as App).kodein.instance()

        setHasOptionsMenu(true)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launchIO {
                    withUIContext { listAdapter.clear() }

                    (requireContext().applicationContext as App).pattonService.messages.collect { msg ->
                        withUIContext { listAdapter.add(msg) }
                    }
                }

                launchIO {
                    (requireContext().applicationContext as App).pattonService.usersOnline.collect { cnt ->
                        withUIContext { binding.clientsOnlineTextView
                            .post {
                                binding.clientsOnlineTextView.text = cnt.toString()
                            }
                        }
                    }
                }

                launchIO {
                    (requireContext().applicationContext as App).pattonService.thingsUpvoted.collect { cnt ->
                        withUIContext { binding.thingsUpvotedTextView
                            .post {
                                binding.thingsUpvotedTextView.text = cnt.toString()
                            }
                        }
                    }
                }

                launchIO {
                    (requireContext().applicationContext as App).pattonService.socketConnected.collect { connected ->
                        withUIContext {
                            if (connected) {
                                binding.pattonTabLayout.post { // putting into the message queue so the UI doesn't freeze
                                    binding.submitLinkButton.isEnabled = true
                                    binding.pattonTabLayout.getTabAt(1)?.select()
                                }
                            } else {
                                binding.pattonTabLayout.post {
                                    binding.submitLinkButton.isEnabled = false
                                    binding.pattonTabLayout.getTabAt(0)?.select()
                                }
                            }

                        }
                    }
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAccount()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy called")
    }

    /**
     *  These functions should be called in Fragment.onViewCreated
     *  in the following order
     */
    override fun setupBinding(view: View) {
        binding = FragmentPattonBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        val offlineTab = binding.pattonTabLayout.newTab()
        val onlineTab = binding.pattonTabLayout.newTab()

        val myList = mutableListOf<String>()
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, myList)

        binding.LogListView.adapter = listAdapter

        binding.LogListView.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL;
        binding.LogListView.isStackFromBottom = true;

        offlineTab.text = "off"
        offlineTab.icon = AppCompatResources.getDrawable(
            requireContext(),
            R.drawable.ic_baseline_radio_button_checked_24
        )

        onlineTab.text = "on"
        onlineTab.icon = AppCompatResources.getDrawable(
            requireContext(),
            R.drawable.ic_baseline_radio_button_unchecked_24
        )

        binding.pattonTabLayout.addTab(offlineTab, 0)
        binding.pattonTabLayout.addTab(onlineTab, 1)

        binding.pattonTabLayout.addOnTabSelectedListener(tabSelectedListener)

        binding.chooseAccountLinearLayout.setOnClickListener {
            bottomSheetAccountsFragment = BottomSheetAccountsFragment { acct ->
                viewModel.setAccount(acct)
                bottomSheetAccountsFragment.dismiss()
            }

            bottomSheetAccountsFragment.show(childFragmentManager, "BottomSheetAccountsFragment")
        }

        binding.submitLinkButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Submit url to a Post or Comment")
                .setPositiveButton("Ok") { dialog, _ ->
                    val str = dialog.inputText(R.id.dialog_text_input_layout)

                    if(str.length < 2000) {
                        val payload = JSONObject().put("url", str)
                        (requireContext().applicationContext as App).pattonService.emitUpvoteSubmission(payload)
                    } else {
                        listAdapter.add("url is too long.") // emit this to Service.messages first?
                    }
                }
                .showInput(
                    layout= R.layout.dialog_text_input,
                    tilId = R.id.dialog_text_input_layout,
                    hintRes = "url"
                )
        }
    }

    override fun setupObservers() {
        viewModel.getAccount().observe(viewLifecycleOwner) {
            (requireContext().applicationContext as App).pattonService.account.tryEmit(it)

            binding.chooseAccountTextView.text = it.name
            launchIO { dataStoreHelper.setSelectedAccountUsername(it.name) }

            Glide
                .with(this)
                .load(it.iconImage)
                .apply(RequestOptions().override(100))
                .circleCrop()
                .error(R.drawable.ic_baseline_image_24)
                .into(binding.chooseAccountImageView)
        }
    }

    /**
     *
     */
    private val tabSelectedListener = object: TabLayout.OnTabSelectedListener {
        /**
         * Called when a tab enters the selected state.
         *
         * @param tab The tab that was selected
         */
        override fun onTabSelected(tab: TabLayout.Tab?) {
            toggleOnOffTabIcons(tab)
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

        }

        /**
         * toggles the off/on tabs
         *
         * [tab] is the tab you want to toggle "on"
         *
         */
        fun toggleOnOffTabIcons(tab: TabLayout.Tab?) {
            tab?.let {
                it.icon = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_radio_button_checked_24
                )

                when(it.position) {
                    0 -> {
                        binding.pattonTabLayout.getTabAt(1)?.let { ttab ->
                            ttab.icon = AppCompatResources.getDrawable(
                                requireContext(),
                                R.drawable.ic_baseline_radio_button_unchecked_24
                            )
                        }

                        (requireContext().applicationContext as App).stopPattonService()

                    }
                    1 -> {
                        binding.pattonTabLayout.getTabAt(0)?.let { ttab ->
                            ttab.icon = AppCompatResources.getDrawable(
                                requireContext(),
                                R.drawable.ic_baseline_radio_button_unchecked_24
                            )
                        }

                        (requireContext().applicationContext as App).startPattonService(viewModel.getAccount().value!!)
                    }
                    else -> {}
                }
            }
        }

    }


    override fun setupRecyclerView() {}

    /**
     *  clear any User input data from this Fragment
     */
    override fun clearUserInputViews() {}
}