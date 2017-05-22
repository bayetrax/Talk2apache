********************************************************************************
**
**  Intel Rockhopper
**  Java
**
********************************************************************************

Using Dynamic Command Resources Generation Tool

based on Rockhopper release: 1.0.0.473

Tgenerate a dynamic command list for Rockhopper Command mode:

two new resources files will be generated in config folder
    ie.
        config/en-US/cl_dynamic.fst
        config/en-US/cl_dynamic.txt

    the full content to the config folder should be as follow:
        config/
        ├── command_asr_main_stream.txt
        └── en-US
            ├── cl_dynamic.fst
            ├── cl_dynamic.txt
            ├── cl.fst
            ├── dynamic_class_ids.txt
            ├── g2p.fst
            ├── g.fst
            ├── rh.dnn
            ├── rh.hmm
            ├── rh.tri
            └── words.txt


copy the entire config folder as resources to be supplied via HUDRecognizerIntent with command mode
    HUDRecognizerIntent.EXTRA_LANGUAGE_RESOURCE_DIR => config
    HUDRecognizerIntent.EXTRA_LANUGAGE_CONFIGURATION_FILE => config/command_asr_main_stream.txt

    ie. 
       $ adb shell mkdir /data/RH
       $ adb push config/en-US /data/RH/en-US
       $ adb push config/command_asr_main_stream.txt /data/RH/command_asr_main_stream.txt

start the recognition, it will only recognition the command specified in the commandlist.txt
    ie.
        Intent recognitionIntent = new Intent(HUDRecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANGUAGE_MODEL, HUDRecognizerIntent.LANGUAGE_MODEL_COMMAND_FORM);
        recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANGUAGE_RESOURCE_DIR, "/data/RH");
        recognitionIntent.putExtra(HUDRecognizerIntent.EXTRA_LANUGAGE_CONFIGURATION_FILE, "/data/RH/command_asr_main_stream.txt");


