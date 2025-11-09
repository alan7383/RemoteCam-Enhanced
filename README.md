<h1 align="center">📸 RemoteCamEnhanced ^^</h1>

<p align="center">
  <img src="https://github.com/user-attachments/assets/remotecamenhanced-banner.png" alt="banner" width="850">
</p>

---

### what is this

RemoteCamEnhanced is a fork of [Ruddle’s RemoteCam](https://github.com/Ruddle/RemoteCam).  
same idea: stream your android camera to your pc, but with a new look and more features.

made with **Material 3 expressive / Monet**, new camera stuff, and some droidcam-like features,  
but still **free**, **no ads**, and **open source** :3

---

### what’s new

- material you ui (dynamic colors, smooth animations)
- support for multiple lenses (wide / telephoto / ultra-wide)
- audio streaming 🎤
- better connection handling (auto reconnect, discovery)
- obs + v4l2 compatible
- cleaner codebase and faster streaming
- still light and simple to use ^^

---

### how it works

you pick a sensor and resolution → app captures jpeg frames →  
they’re sent over http as an mjpeg stream to your computer.

open this on your pc:

```

http://<your-phone-ip>:8080/cam.mjpeg

````

works directly in browsers, obs, or ffmpeg.

<img src="assets/screen_remotecam.jpg" width="400">

---

### download

👉 [**download remotecamenhanced.apk**](https://github.com/alan7383/remotecamenhanced/releases)

not on play store

---

### use cases

**in obs:**  
add a browser or media source →  
paste your stream url (like `http://192.168.x.x:8080/mjpeg`)  
browser source gives lower latency.

![screenshot](assets/obs_mediasource.png)

**linux (v4l2):**
```bash
ffmpeg -i http://192.168.x.x:8080/mjpeg -f v4l2 /dev/video0
````

---

### why

droidcam is fine, but hd and multi-lens are locked behind paywalls.
remotecam was open but pretty basic.
so remotecamenhanced is a modern, open version with a clean ui,
better camera control, and material you all the way :)

---

### license

based on [RemoteCam](https://github.com/Ruddle/RemoteCam) (MIT)
this fork is also under the **MIT license**

---

<p align="center">
  made with ☕ and a bit of chaos by <a href="https://github.com/alan7383">alan7383</a> (´･ω･`)
</p>
