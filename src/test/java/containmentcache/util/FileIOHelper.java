package containmentcache.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Helper methods for the manipulation of file input/output, including serialized objects.
 * @author afrechet
 */
public class FileIOHelper {

    private FileIOHelper()
    {
        //Cannot construct an instance of a helper class.
    }

    /**
     * Create a directory at the specified location.
     * @param aDirectoryLocation - location to create directory at.
     * @param aOverwrite - whether to overwrite if the folder already exists.
     * @return the directory file created.
     * @throws IllegalStateException - if the directory file creation failed.
     * @throws IllegalArgumentException - if the directory already exists and we are not allowed to overwrite.
     */
    public static File createDirectory(String aDirectoryLocation, boolean aOverwrite)
    {
        File directory = new File(aDirectoryLocation);

        if(directory.exists())
        {
            if (!aOverwrite)
            {
                throw new IllegalArgumentException("Directory at "+aDirectoryLocation+" already exists and not allowed to overwrite.");
            }
        }
        else
        {
            if(!directory.mkdirs())
            {
                throw new IllegalStateException("Failed to create execution directory at "+aDirectoryLocation);
            }
        }


        return directory;
    }

    /**
     * Read a serializable object casting it to the provided return value.
     *
     * @param <T> - the (expected) serialized object type.
     * @param aFilename - the filename of a serialized object.
     * @return the serialized object read cast to the required class.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T readSerializedObject(String aFilename) throws IOException
    {
        ObjectInput reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(aFilename)));
        try
        {
            try
            {
                return (T) reader.readObject();
            }
            catch(ClassNotFoundException e)
            {
                e.printStackTrace();
                throw new IllegalArgumentException("Could not read in serialized object at "+aFilename+" ("+e.getMessage()+").");
            }
        }
        finally
        {
            reader.close();
        }
    }

    /**
     * Write a serializable object to a file.
     *
     * @param <T> - the type of serialized object to write to file.
     * @param aFilename - the filename to write the serializable object to.
     * @param aObject - the serializable object to write.
     * @throws IOException
     */
    public static <T extends Serializable> void writeSerializedObject(String aFilename, T aObject) throws IOException
    {
        ObjectOutput writer = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(aFilename)));
        try
        {
            writer.writeObject(aObject);
        }
        finally
        {
            writer.close();
        }
    }

}