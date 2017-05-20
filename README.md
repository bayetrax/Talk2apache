# Talk2apache
This App is combination of Wake on Voice and Speech Recognition

Hotword = "OK RECON" of Wake on Voice, once detected starts the speech recognition feature.

-->copy the entire config folder as resources to be supplied via HUDRecognizerIntent with command mode
    HUDRecognizerIntent.EXTRA_LANGUAGE_RESOURCE_DIR => RH-Command
    HUDRecognizerIntent.EXTRA_LANUGAGE_CONFIGURATION_FILE => RH-Command/command_asr_main_stream.txt

    ie. 
       $ adb shell mkdir /data/RH
       $ adb push RH-Resources/RH-Command/en-US /data/RH/en-US
       $ adb push RH-Resources/RH-Command/command_asr_main_stream.txt /data/RH/command_asr_main_stream.txt

-->start the recognition, it will only recognition the command specified in the commandlist.txt
    ie.
        Intent recognitionIntent = new Intent(HUDRecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANGUAGE_MODEL, HUDRecognizerIntent.LANGUAGE_MODEL_COMMAND_FORM);
        recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANGUAGE_RESOURCE_DIR, "/data/RH");
        recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANUGAGE_CONFIGURATION_FILE, "/data/RH/command_asr_main_stream.txt");

