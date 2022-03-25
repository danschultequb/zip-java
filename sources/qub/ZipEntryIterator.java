package qub;

public class ZipEntryIterator implements Iterator<ZipEntryReadStream>, Disposable
{
    private final Function0<java.util.zip.ZipInputStream> zipInputStreamCreator;
    private java.util.zip.ZipInputStream zipInputStream;
    private ZipEntryReadStream current;
    private boolean isDisposed;

    private ZipEntryIterator(Function0<java.util.zip.ZipInputStream> zipInputStreamCreator)
    {
        PreCondition.assertNotNull(zipInputStreamCreator, "zipInputStreamCreator");
        
        this.zipInputStreamCreator = zipInputStreamCreator;
    }

    public static ZipEntryIterator create(File file)
    {
        PreCondition.assertNotNull(file, "file");

        return ZipEntryIterator.create(() -> { return file.getContentsReadStream().await(); });
    }

    public static ZipEntryIterator create(ByteReadStream readStream)
    {
        PreCondition.assertNotNull(readStream, "readStream");

        return ZipEntryIterator.create(() -> { return readStream; });
    }

    public static ZipEntryIterator create(Function0<? extends ByteReadStream> readStreamCreator)
    {
        PreCondition.assertNotNull(readStreamCreator, "readStreamCreator");

        return new ZipEntryIterator(() ->
        {
            final ByteReadStream readStream = readStreamCreator.run();
            final java.io.InputStream contentsInputStream = ByteReadStreamToInputStream.create(readStream);
            return new java.util.zip.ZipInputStream(contentsInputStream);
        });
    }

    @Override
    public boolean hasStarted()
    {
        return this.zipInputStream != null;
    }

    @Override
    public boolean hasCurrent()
    {
        return this.current != null;
    }

    @Override
    public ZipEntryReadStream getCurrent()
    {
        PreCondition.assertTrue(this.hasCurrent(), "this.hasCurrent()");

        return this.current;
    }

    @Override
    public boolean next()
    {
        if (this.zipInputStream == null)
        {
            this.zipInputStream = this.zipInputStreamCreator.run();
        }
        else if (this.current != null)
        {
            this.current.dispose().await();
        }

        final java.util.zip.ZipEntry javaZipEntry;
        try
        {
            javaZipEntry = this.zipInputStream.getNextEntry();
        }
        catch (Exception e)
        {
            throw Exceptions.asRuntime(e);
        }

        if (javaZipEntry == null)
        {
            this.dispose().await();
        }
        else
        {
            final InputStreamToByteReadStream zipByteReadStream = InputStreamToByteReadStream.create(this.zipInputStream);
            final CharacterToByteReadStream characterToByteReadStream = CharacterToByteReadStream.create(zipByteReadStream);
            // It's important that the CharacterToByteReadStream we pass to the
            // ZipEntryCharacterToByteReadStream ignores requests to dispose since it is
            // directly referring to this object's zipInputStream. This object's zipInputStream
            // should only be disposed when this object is disposed.
            final IgnoreDisposeCharacterToByteReadStream ignoreDisposeReadStream = IgnoreDisposeCharacterToByteReadStream.create(characterToByteReadStream);
            this.current = ZipEntryReadStream.create(javaZipEntry, ignoreDisposeReadStream);
        }

        return this.hasCurrent();
    }

    @Override
    public boolean isDisposed()
    {
        return this.isDisposed;
    }

    @Override
    public Result<Boolean> dispose()
    {
        return Result.create(() ->
        {
            final boolean result = !this.isDisposed;
            if (result)
            {
                this.isDisposed = true;

                if (this.current != null)
                {
                    this.current.dispose().await();
                    this.current = null;
                }

                try
                {
                    this.zipInputStream.close();
                }
                catch (Exception e)
                {
                    throw Exceptions.asRuntime(e);
                }
            }
            return result;
        });
    }
}
