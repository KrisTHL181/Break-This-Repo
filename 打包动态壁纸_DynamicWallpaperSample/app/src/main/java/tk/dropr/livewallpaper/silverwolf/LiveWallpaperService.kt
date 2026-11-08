package tk.dropr.livewallpaper.silverwolf

import android.app.WallpaperManager
import android.media.MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
import android.media.MediaPlayer
import android.media.browse.MediaBrowser
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer


class LiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = WallpaperEngine()

    inner class WallpaperEngine : WallpaperService.Engine() {
        lateinit var player :ExoPlayer

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            player = ExoPlayer.Builder(applicationContext).build()
            player.setVideoSurface(holder!!.surface)
            val mediaItem = MediaItem.fromUri("asset:///livewallapaper.mp4")
            //player.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

            player.setRepeatMode(Player.REPEAT_MODE_ALL)
            player.setVolume(0f)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            /*mediaPlayer = MediaPlayer.create(applicationContext, R.raw.livewallapaper).also {
                it.isLooping = true
            }

            mediaPlayer.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)

            mediaPlayer.setSurface(holder!!.surface)
            mediaPlayer.start()*/
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

        }
        /*override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle {
            try {
                Log.d("xys", "onCommand: $action----$x---$y---$z")
                if ("android.wallpaper.tap" == action) {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }*/

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                if(this::player.isInitialized){
                }
                player.play()
            } else {
                player.pause()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }

        }
    }