# Simple WebSocket server with Play! Framework and Scala


A very simple WebSocket server using Scala with the Play! Framework. 
####I wrote an explanation post to go with this [on my blog](http://ticofab.io/simple-play-websocket-server/).


This example illustrates how to create WebSocket endpoints and usage of:

* Enumerator.fromFile
* Enumerator.eof
* Enumerator.repetM
* Enumerator.generateM
* Enumerator.interleave
* Iteratee.foreach
* Iteratee.ignore
* Concurrent.unicast
* Channel push

You can find a simple client to test this at my repository [Simple Websocket Client](https://github.com/ticofab/simple-websocket-client). I wrote a presentation about WebSockets in Play, you can find it [here on SlideShare](http://www.slideshare.net/FabioTiriticco/websocket-wiith-scala-and-play-framework). 

## LICENSE

This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with
the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

