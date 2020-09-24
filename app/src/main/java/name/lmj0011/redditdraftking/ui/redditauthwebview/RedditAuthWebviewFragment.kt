package name.lmj0011.redditdraftking.ui.redditauthwebview

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
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
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import org.kodein.di.instance
import timber.log.Timber

class RedditAuthWebviewFragment : Fragment() {

    private lateinit var redditAuthHelper: RedditAuthHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_reddit_auth_webview, container, false)
        val browser: WebView = root.findViewById(R.id.reddit_auth_webview)
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        browser.webChromeClient = WebChromeClient()
        setHasOptionsMenu(true)

        browser.webViewClient = object : WebViewClient(){
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (redditAuthHelper.authClient.isRedirectedUrl(url)) {
                    browser.stopLoading()

                    // We will retrieve the bearer on the background thread.
                    GlobalScope.launch{
                        try {
                          redditAuthHelper.authClient.getTokenBearer(url)
                        } catch (ex: AccessDeniedException) {
                            redditAuthHelper.authClient.getSavedBearer().revokeToken()
                            Timber.e(ex)
                        }

                        withContext(Dispatchers.Main){
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }

        browser.settings.javaScriptEnabled = true
        browser.loadUrl(redditAuthHelper.authClient.provideAuthorizeUrl())

        return root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.findItem(R.id.action_auth_app).isVisible = false
    }

}