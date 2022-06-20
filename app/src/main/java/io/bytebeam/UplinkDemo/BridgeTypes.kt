import android.os.Parcelable
import io.bytebeam.uplink.generated.UplinkAction
import kotlinx.parcelize.Parcelize

@Parcelize
class UplinkActionBridge(
    val id: String,
    val name: String,
    val payload: String,
): Parcelable {
    companion object {
        fun from(uplinkAction: UplinkAction): UplinkActionBridge {
            return UplinkActionBridge(
                uplinkAction.id,
                uplinkAction.name,
                uplinkAction.payload
            )
        }
    }
}