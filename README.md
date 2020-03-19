# MFCC_Extractor_TarsosDSP
MFCCs extractor utilizing TarsosDSP library and save the results into .h5 file for further use.

This Java class utilizes the TarsosDSP library to extract MFCC features out of audio files listed on a given dataset. TarsosDSP library offers several methods in `AudioDispatcherFactory` to perform the audio sampling, in this case, I used the `AudioDispatcherFactory.fromPipe` method which relies on **ffmpeg**. I haven't figured out the equation behind this library regarding the calculation to extract the desired amount of frame from a single audio file (Please, don't call me lazy for not tracing the code; I got a more significant issue to deal with). In my case, I need to extract 612 frames in a six-seconds long audio file, and I rely on *trial-and-error* to determine the frame and overlap size to pass to the `AudioDispatcher`.

## Here's how it works ##
1. The code starts with asking for user input to determine the extracted frame size, which are:
  * 612 frames x 40 MFCCs
  * 102 frames x 40 MFCCs
2. In this project, I only provide two audio datasets: *UrbanSound8K* and *Freesound*; hence the program extracts these features sequentially from UrbanSound8K-->Freesound(train)-->Freesound(test)

> Note: UrbanSound8K doesn't divide its dataset into train and test; instead, it separates its audio files in ten folders (fold1, fold2, et cetera). In my case, I choose *fold10* as the test data.
3. Each sequent of extraction calls `arrayHandler` method to save the extracted MFCCs from `extractMFCC` method into the `trainData` or `testData` for further use. This method also saves the labels extracted from the datasets to `trainLabel` and `testLabel`. After each audio file extraction, I call the `killDispatcher()` to free the resources up
4. In the 'extractMFCC` method, there are several parameters we need to determine. Here are the values for each needed parameters in my case:

| Parameters | Values |
|------------|-------:|
| Sample rate | 8000 Hz |
| Frame size | 400 samples | 
| Frame overlap | 322 samples |
| Cepstrum coefficient | 40 units |
| Mel filter | 40 mels |
| Lower frequency | 0 Hz |
| Upper frequency | 4000 Hz |

5. In the `extractMFCC` method, I also do the *zero-padding* (yes, I know it's unnecessary) and put a frame limit to stop the extraction when it reached the number of frames we desire. The frame clipping process occurs when it receives an audio file with a longer duration (more than 6 seconds)
6. After it finishes all the extraction processes, it calls the `writeToH5` method. This method relies on **jarhdf5-1.10.5.jar** library to create, write and close the .h5 file
7. This method starts with creating the .h5 file, then creates the data space to holds the datasets inside the file, then creates the datasets. In my case, I create two datasets in each file called *data* and *label*
8. After the creations, it starts writing all the extracted features and labels to the corresponding datasets
9. When it completes everything without error, it closes the resources such as datasets, data spaces, and files

> The program utilizes **opencsv:5.1**, **jarhdf5-1.10.5**, and  **TarsosDSP-2.4**.

I hope this piece of code helps you, and may God be with you. :angel:
