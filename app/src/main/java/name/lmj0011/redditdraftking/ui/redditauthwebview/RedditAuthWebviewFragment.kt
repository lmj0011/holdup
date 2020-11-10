package name.lmj0011.redditdraftking.ui.redditauthwebview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kirkbushman.auth.RedditAuth
import com.kirkbushman.auth.errors.AccessDeniedException
import com.kirkbushman.auth.managers.SharedPrefsStorageManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.util.apiPath
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.ui.home.HomeViewModel
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber

class RedditAuthWebviewFragment : Fragment() {

    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper
    private val  viewModel by viewModels<RedditAuthWebviewViewModel> {
        ViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_reddit_auth_webview, container, false)
        val browser: WebView = root.findViewById(R.id.reddit_auth_webview)
        var username = ""
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
        setHasOptionsMenu(true)

        browser.settings.javaScriptEnabled = true
        browser.webChromeClient = WebChromeClient()

        browser.webViewClient = object : WebViewClient(){
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (redditAuthHelper.authClient().isRedirectedUrl(url)) {
                    browser.stopLoading()

                    // We will retrieve the bearer on the background thread.
                    launchIO{
                        val account = viewModel.createNewAccount(username)

                        try {
                          account?.let {
                             redditAuthHelper.authClient(it).getTokenBearer(url)
                              val res = redditApiHelper.get("user/${username.substring(2)}/about", redditAuthHelper.authClient(it).getSavedBearer().getAccessToken()!!)
                              val json = JSONObject(res.body!!.source().readUtf8())

                              try {
                                  val iconImg = json.getJSONObject("data").getString("icon_img")
                                  if (!iconImg.isNullOrBlank()) account.iconImage = iconImg.split("?")[0]

                                  try {
                                      val snooImg = json.getJSONObject("data").getString("snoovatar_img")
                                      if (!snooImg.isNullOrBlank()) account.iconImage = snooImg.split("?")[0]
                                  }
                                  catch (ex: org.json.JSONException) {
                                      Timber.i("No snoovatar for this Account: $account")
                                  }
                              }
                              catch (ex: org.json.JSONException) {
                                  Timber.w("Unable to set icon_img on this Account: $account")
                              }
                              finally {
                                  viewModel.updateAccount(account)
                              }
                          }
                        } catch (ex: AccessDeniedException) {
                            account?.let {
                                redditAuthHelper.authClient(it).getSavedBearer().revokeToken()
                                viewModel.deleteAccount(it)
                            }
                            Timber.e(ex)
                        }

                        withContext(Dispatchers.Main){
                            findNavController().navigateUp()
                        }
                    }
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                // try to grab the username to assign to an `Account`
                val query = "document.querySelector('body > div.content > div > h1 > a').innerText;"
                view.evaluateJavascript(query) { queryResult ->
                    Timber.d("queryResult: $queryResult")
                    if(queryResult != "null") username = queryResult.replaceFirst("\"", "u/").replace("\"", "")
                }
            }
        }

        browser.settings.javaScriptEnabled = true
        browser.loadUrl(redditAuthHelper.authClient().provideAuthorizeUrl())

        return root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.action_manage_accounts).isVisible = false
    }

}

