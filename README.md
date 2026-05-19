# Private Android Apps

This repo contains my private Android Apps. Main purpose for this repo is to keep my code safe and store, but feel free to use this as an fork/example for your apps.

You won't find anything groundbreaking here, it's just a developers repo that is customizing his devices to their liking.

## "Simply Time" Watchface

Simple watchface showing time in a digital clock with a small twist from the time I was hyped around Tom Clancy's The Division.

Since I'm using a font that is free for private use I'm simply not adding it to the repo. For you to run it, you need to get two fonts and put them into wear-watchface wear-watchface/app/src/main/res/font folder under names digital.ttf and digital_empty.ttf (customizable in code). First one is the main font, the second is used when the watch screen is in ambient mode. You need digits for the time and letters that will be used is short for of the day (mon, tue, etc.).

## Homelab Share

Android (phone) app adding activity to share context that uploads the data to a http server.

Make sure that `values/strings.xml` points to the right url.