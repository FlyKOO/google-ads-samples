package com.aospstudio.sample.admob.ads

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.aospstudio.sample.admob.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private const val AD_COUNTER_TIME = 5L

class AdDialogFragment : DialogFragment() {

    private var listener: AdDialogInteractionListener? = null
    private var countDownTimer: CountDownTimer? = null
    private val timeRemainingState = mutableStateOf(AD_COUNTER_TIME)

    fun setAdDialogInteractionListener(listener: AdDialogInteractionListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    AdDialogContent(timeRemainingState.value)
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(this.requireActivity())
        builder.setView(composeView)

        val args = arguments
        var rewardAmount = -1
        var rewardType: String? = null
        if (args != null) {
            rewardAmount = args.getInt(REWARD_AMOUNT)
            rewardType = args.getString(REWARD_TYPE)
        }
        if (rewardAmount > 0 && rewardType != null) {
            builder.setTitle(getString(R.string.reward_title))
        }

        builder.setNegativeButton(
            getString(R.string.negative_button_text)
        ) { _, _ -> dialog?.cancel() }
        val dialog: Dialog = builder.create()
        createTimer(AD_COUNTER_TIME)
        return dialog
    }

    private fun createTimer(time: Long) {
        timeRemainingState.value = time
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(time * 1000, 50) {
            override fun onTick(millisUnitFinished: Long) {
                val timeRemaining = millisUnitFinished / 1000 + 1
                timeRemainingState.value = timeRemaining
            }

            override fun onFinish() {
                dialog?.dismiss()

                listener?.onShowAd()
            }
        }
        countDownTimer?.start()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onCancelAd()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        countDownTimer = null
    }

    interface AdDialogInteractionListener {
        fun onShowAd()

        fun onCancelAd()
    }

    companion object {
        private const val REWARD_AMOUNT = "rewardAmount"
        private const val REWARD_TYPE = "rewardType"

        fun newInstance(rewardAmount: Int, rewardType: String): AdDialogFragment {
            val args = Bundle()
            args.putInt(REWARD_AMOUNT, rewardAmount)
            args.putString(REWARD_TYPE, rewardType)
            val fragment = AdDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}

@Composable
private fun AdDialogContent(timeRemaining: Long) {
    Text(
        text = stringResource(R.string.video_starting_in_text, timeRemaining),
        style = MaterialTheme.typography.body1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)
    )
}
