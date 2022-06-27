import 'dart:async';
import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:flutter_accessibility_service/accessibility_event.dart';

import 'package:flutter_accessibility_service/flutter_accessibility_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  StreamSubscription? _subscription;
  List<Map<String,dynamic>> events = [];
  final ScrollController _scrollController = ScrollController();
  @override
  void initState() {
    super.initState();
    _startEventListen();
  }

  _startEventListen(){
    if (_subscription?.isPaused ?? false) {
      _subscription?.resume();
      return;
    }
    _subscription = FlutterAccessibilityService.eventStream.listen((event) {
      debugPrint('---> Accessibility event: ${event}');
      var eventType = event['eventType'];
      var keyCodeType = event['keyCodeType'];
      var keyCodeActionType = event['keyCodeActionType'];
      if (eventType == 'keyCode'){
        if (keyCodeType == 'KEYCODE_DPAD_CENTER' && keyCodeActionType == 'singleClick'){
          FlutterAccessibilityService.performGlobalAction(actionType: 'GLOBAL_ACTION_HOME');
        }
        if (keyCodeType == 'KEYCODE_DPAD_CENTER' && keyCodeActionType == 'doublePress'){
          FlutterAccessibilityService.performGlobalAction(actionType: 'GLOBAL_ACTION_RECENTS');
        }
      }
      WidgetsBinding.instance?.addPostFrameCallback((timeStamp) {
        if (mounted){
          _scrollController.animateTo(
            _scrollController.position.maxScrollExtent,
            duration: const Duration(seconds: 1),
            curve: Curves.linear,
          );
        }
      });
      setState(() {
        events.add(event);
      });
    });
  }
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    TextButton(
                      onPressed: () async {
                        final bool res = await FlutterAccessibilityService.isAccessibilityPermissionEnabled();
                        debugPrint('---> Is enabled: $res');
                      },
                      child: const Text("Check Permission"),
                    ),
                    const SizedBox(height: 20.0),

                    TextButton(
                      onPressed: () async {
                        await FlutterAccessibilityService.requestAccessibilityPermission();
                      },
                      child: const Text("Request Permission"),
                    ),
                    const SizedBox(height: 20.0),

                    TextButton(
                      onPressed: () async {
                        final bool res = await FlutterAccessibilityService.isServiceRunning();
                        debugPrint('---> Is Running res: ${res}');
                      },
                      child: const Text("Check Is Running"),
                    ),
                    const SizedBox(height: 20.0),

                    TextButton(
                      onPressed: () async {
                        final bool res = await FlutterAccessibilityService.startService();
                        debugPrint('---> Is Running res: ${res}');
                      },
                      child: const Text("startService"),
                    ),
                    const SizedBox(height: 20.0),


                    TextButton(
                      onPressed: _startEventListen,
                      child: const Text("Start Stream"),
                    ),
                    const SizedBox(height: 20.0),
                    TextButton(
                      onPressed: () {
                        _subscription?.cancel();
                      },
                      child: const Text("Stop Stream"),
                    ),

                    TextButton(
                      onPressed: () async {
                        final bool res = await FlutterAccessibilityService.performGlobalAction(actionType: 'GLOBAL_ACTION_RECENTS');
                        debugPrint('---> Is Running res: ${res}');
                      },
                      child: const Text("RECENTS"),
                    ),
                    const SizedBox(height: 20.0),

                    TextButton(
                      onPressed: () async {
                        final bool res = await FlutterAccessibilityService.performGlobalAction(actionType: 'GLOBAL_ACTION_HOME');
                        debugPrint('---> Is Running res: ${res}');
                      },
                      child: const Text("HOME"),
                    ),
                    const SizedBox(height: 20.0),

                    TextButton(
                      onPressed: () async {
                        final bool res = await FlutterAccessibilityService.performGlobalAction(actionType: 'GLOBAL_ACTION_BACK');
                        debugPrint('---> Is Running res: ${res}');
                      },
                      child: const Text("BACK"),
                    ),
                    const SizedBox(height: 20.0),
                  ],
                ),
              ),
              Expanded(
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: events.length,
                  controller: _scrollController,
                  itemBuilder: (_, index) => ListTile(
                    title: Text(events[index].toString()),
                    // subtitle: Text(events[index]!.capturedText ?? ""),
                  ),
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
