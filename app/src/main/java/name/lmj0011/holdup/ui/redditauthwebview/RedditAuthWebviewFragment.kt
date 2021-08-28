package name.lmj0011.holdup.ui.redditauthwebview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kirkbushman.auth.errors.AccessDeniedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.helpers.RedditApiHelper
import name.lmj0011.holdup.helpers.RedditAuthHelper
import name.lmj0011.holdup.helpers.factories.ViewModelFactory
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.showSnackBar
import name.lmj0011.holdup.BuildConfig
import name.lmj0011.holdup.helpers.util.openUrlInWebBrowser
import org.json.JSONObject
import org.kodein.di.instance
import timber.log.Timber

class RedditAuthWebviewFragment : Fragment() {
    private lateinit var rootView: View
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
    ): View {
        rootView = inflater.inflate(R.layout.fragment_reddit_auth_webview, container, false)
        val browser: WebView = rootView.findViewById(R.id.reddit_auth_webview)
        var username = ""
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
        setHasOptionsMenu(true)

        browser.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let{ consoleMsg ->
                    when(consoleMsg.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> {
                            val msg = "webChromeClient JS error: ${consoleMsg.message()}"
                            Timber.e(msg)
                        }
                        else -> {
                            if (BuildConfig.FLAVOR == "preview") {
                                val msg = "webChromeClient console message: ${consoleMsg.message()}"
                                Timber.d(msg)
                            }

                        }
                    }
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        browser.webViewClient = object : WebViewClient(){
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (redditAuthHelper.authClient().isRedirectedUrl(url)) {
                    browser.stopLoading()

                    launchIO{
                        getAccountTokenBearer(username, url)

                        withContext(Dispatchers.Main){
                            findNavController().navigateUp()
                        }
                    }
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                // try to grab the username to assign to an `Account`
                val query = """
                    (function () {
                        const ele = document.querySelector('body > div.content > div > h1 > a');
                    
                        if (ele !== null) { return ele.innerText; }
                        else return null;
                    })();
                    """.trimIndent()
                view.evaluateJavascript(query) { queryResult ->
                    Timber.d("queryResult: $queryResult")
                    if(queryResult != "null") username = queryResult.replaceFirst("\"", "u/").replace("\"", "")
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                val msg = error?.description.toString()

                Timber.e(msg)

                if (BuildConfig.FLAVOR == "preview") {
                    if(msg.isNotBlank()) showSnackBar(rootView, msg)
                    else showSnackBar(rootView, "WebViewClient encountered an error, errorCode: ${error?.errorCode}")
                }

                super.onReceivedError(view, request, error)
            }
        }

        browser.settings.javaScriptEnabled = true
        browser.loadUrl(redditAuthHelper.authClient().provideAuthorizeUrl())

        rootView.findViewById<Button>(R.id.buttonSubmit).setOnClickListener {
            var user = rootView.findViewById<EditText>(R.id.editTextTextUsername).text.toString()
            val url = rootView.findViewById<EditText>(R.id.editTextCallbackUrl).text.toString()

            if(user.substring(0,2) != "u/") {
                user = "u/$user"
            }

            rootView.findViewById<LinearLayout>(R.id.manualCredentialsInputContainer)
                .visibility = View.GONE

            launchIO{
                getAccountTokenBearer(user, url)

                withContext(Dispatchers.Main){
                    findNavController().navigateUp()
                }
            }
        }

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.reddit_auth, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_open_in_browser -> {
                openUrlInWebBrowser(requireContext(), redditAuthHelper.authClient().provideAuthorizeUrl())
                true
            }
            R.id.action_enter_callback_url -> {
                rootView.findViewById<LinearLayout>(R.id.manualCredentialsInputContainer)
                    .visibility = View.VISIBLE

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    suspend fun getAccountTokenBearer(username: String, callbackUrl: String) {
        val account = viewModel.createNewAccount(username)

        try {
            account?.let {
                redditAuthHelper.authClient(it).getTokenBearer(callbackUrl)
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
                        showSnackBar(rootView, "No snoovatar found for ${account.name}")
                    }
                }
                catch (ex: org.json.JSONException) {
                    showSnackBar(rootView, "Unable to set icon_img on this Account: ${account.name}")
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
            showSnackBar(rootView, ex.message.toString())
        }
    }

}

