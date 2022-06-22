package io.bytebeam.UplinkDemo

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger

class UplinkService: Service() {
    //    // forward events to these subscribers
//    val subscribers = mutableListOf<Messenger>()
//
//    lateinit var uplink: Uplink
//
//    override fun onBind(intent: Intent?): IBinder? {
//        val configString = intent?.getStringExtra(UPLINK_CONFIG_KEY)!!
//        val config = UplinkConfig(configString)
//        uplink = Uplink(config)
//        uplink.subscribe(this::uplinkSubscription)
//        return Messenger(Handler(this::handleMessage)).binder
//    }
//
//    private fun uplinkSubscription(action: UplinkAction) {
//        subscribers.forEach {
//            it.send(Message().apply {
//                what = 0
//                obj = UplinkActionBridge.from(action)
//            })
//        }
//    }
//
//    private fun handleMessage(msg: Message): Boolean {
//        when (msg.what) {
//            SEND_DATA -> {}
//            RESPOND_TO_ACTION -> {
//                uplink.respond()
//            }
//            SUBSCRIBE -> {
//                subscribers.add(msg.replyTo)
//            }
//            else -> throw IllegalArgumentException()
//        }
//        return false
//    }
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}

const val UPLINK_CONFIG_KEY = "uplinkConfig"

const val SEND_DATA = 0
const val RESPOND_TO_ACTION = 1
const val SUBSCRIBE = 2
/// for testing, will trigger a segmentation fault
const val CRASH = 3