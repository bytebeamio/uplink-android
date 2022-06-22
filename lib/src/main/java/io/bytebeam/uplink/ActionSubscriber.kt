package io.bytebeam.uplink

interface ActionSubscriber {
    fun processAction(action: UplinkAction): Void?
}