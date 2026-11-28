import { useEffect, useRef } from 'react'

const VIDEO_SRC = '/bg/space.mp4'
const POSTER_SRC = '/bg/space-poster.jpg'

/**
 * Full-screen looping video that sits behind the whole UI.
 * The blur lives in CSS (`--bg-blur` in styles.css), so it can be tuned without re-encoding the video.
 * With "reduce motion" enabled in the OS the video is paused and only the poster frame is shown.
 */
export default function Background() {
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const query = window.matchMedia('(prefers-reduced-motion: reduce)')
    const sync = () => {
      if (query.matches) {
        video.pause()
      } else {
        void video.play().catch(() => {
          // autoplay can be blocked; the poster frame stays visible, which is fine
        })
      }
    }

    sync()
    query.addEventListener('change', sync)
    return () => query.removeEventListener('change', sync)
  }, [])

  return (
    <div className="bg" aria-hidden="true">
      <video
        ref={videoRef}
        className="bg-video"
        src={VIDEO_SRC}
        poster={POSTER_SRC}
        autoPlay
        muted
        loop
        playsInline
        preload="auto"
        tabIndex={-1}
      />
      <div className="bg-overlay" />
    </div>
  )
}
