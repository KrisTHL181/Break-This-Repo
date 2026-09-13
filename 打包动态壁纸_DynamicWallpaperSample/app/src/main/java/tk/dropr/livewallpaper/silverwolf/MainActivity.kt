package tk.dropr.livewallpaper.silverwolf

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.core.content.ContextCompat.startActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import tk.dropr.livewallpaper.silverwolf.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var player : ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(applicationContext).build()

        var mplayer =findViewById<SurfaceView>(R.id.surfaceVideo)
        player.setVideoSurface(mplayer.holder.surface)
        val uri = "asset:///livewallapaper.mp4"
        val mediaItem = MediaItem.fromUri("asset:///livewallapaper.mp4")
        //player.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

        player.setRepeatMode(Player.REPEAT_MODE_ALL)
        player.setVolume(0f)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        }

    public fun btnPressed(view: View)
    {
        val intent = Intent()
        intent.action = WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this,LiveWallpaperService::class.java.name)
        )
        startActivity(intent)
    }
}



