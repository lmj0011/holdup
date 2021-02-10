package name.lmj0011.redditdraftking.ui.submission.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.database.models.Account
import name.lmj0011.redditdraftking.databinding.BottomsheetFragmentAccountsBinding
import name.lmj0011.redditdraftking.helpers.adapters.AccountListAdapter
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.ui.accounts.AccountsViewModel
import timber.log.Timber

class BottomSheetAccountsFragment(private val setAccountForSubmission: (account: Account) -> Unit): BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentAccountsBinding
    private val  accountsViewModel by viewModels<AccountsViewModel> {
        ViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application)
    }
    private lateinit var listAdapter: AccountListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottomsheet_fragment_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
        refreshRecyclerView()
    }

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentAccountsBinding.bind(view)
        binding.lifecycleOwner = this
    }

    private fun setupRecyclerView() {
        listAdapter = AccountListAdapter(
            AccountListAdapter.LogOutClickListener { acct ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Logout ${acct.name}?")
                    .setPositiveButton("Logout") { _, _ ->
                        launchIO {
                            accountsViewModel.deleteAccount(acct)
                            refreshRecyclerView()
                        }
                    }
                    .setNeutralButton("Cancel") {_, _ -> }
                    .show()
            },
            AccountListAdapter.AccountNameClickListener(setAccountForSubmission)
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.accountList.addItemDecoration(decor)
        binding.accountList.adapter = listAdapter
    }

    private fun refreshRecyclerView() {
        launchIO {
            val accounts = accountsViewModel.getAccounts()

            withContext(Dispatchers.Main) {
                listAdapter.submitList(accounts)
                listAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupObservers() {
        binding.addAccountLinearLayout.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().removeAllCookies{
                    if(it) Timber.d("webview Cookies were successfully cleared!")
                    else Timber.d("webview Cookies COULD NOT be cleared!")
                }
            } else CookieManager.getInstance().removeAllCookie()
            findNavController().navigate(R.id.redditAuthWebviewFragment)
        }
    }
}