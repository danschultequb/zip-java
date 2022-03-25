package qub;

public class ZipWriteStream implements Disposable
{
    private final java.util.zip.ZipOutputStream zipOutputStream;
    private ZipEntryWriteStream currentEntryWriteStream;
    private boolean disposed;

    private ZipWriteStream(java.util.zip.ZipOutputStream zipOutputStream)
    {
        PreCondition.assertNotNull(zipOutputStream, "zipOutputStream");

        this.zipOutputStream = zipOutputStream;
    }

    public static Result<ZipWriteStream> create(File file)
    {
        PreCondition.assertNotNull(file, "file");

        return Result.create(() ->
        {
            final ByteWriteStream byteWriteStream = file.getContentsByteWriteStream().await();
            final BufferedByteWriteStream bufferedByteWriteStream = BufferedByteWriteStream.create(byteWriteStream);
            return ZipWriteStream.create(bufferedByteWriteStream);
        });
    }

    public static ZipWriteStream create(ByteWriteStream writeStream)
    {
        PreCondition.assertNotNull(writeStream, "writeStream");

        final ByteWriteStreamToOutputStream outputStream = ByteWriteStreamToOutputStream.create(writeStream);
        final java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(outputStream);
        return new ZipWriteStream(zipOutputStream);
    }

    @Override
    public boolean isDisposed()
    {
        return this.disposed;
    }

    @Override
    public Result<Boolean> dispose()
    {
        return Result.create(() ->
        {
            final boolean result = !this.disposed;
            if (result)
            {
                this.disposed = true;

                if (this.currentEntryWriteStream != null)
                {
                    this.currentEntryWriteStream.dispose().await();
                }

                try
                {
                    this.zipOutputStream.close();
                }
                catch (Exception e)
                {
                    throw Exceptions.asRuntime(e);
                }
            }
            return result;
        });
    }

    private void closeCurrentEntry()
    {
        PreCondition.assertNotNull(this.currentEntryWriteStream, "this.currentEntryWriteStream");

        this.currentEntryWriteStream = null;

        try
        {
            this.zipOutputStream.closeEntry();
        }
        catch (Exception e)
        {
            throw Exceptions.asRuntime(e);
        }

        PostCondition.assertNull(this.currentEntryWriteStream, "this.currentEntryWriteStream");
    }
    
    public ZipEntryWriteStream createEntryWriteStream(String entryPath)
    {
        PreCondition.assertNotNullAndNotEmpty(entryPath, "entryPath");

        return this.createEntryWriteStream(Path.parse(entryPath));
    }

    public ZipEntryWriteStream createEntryWriteStream(Path entryPath)
    {
        PreCondition.assertNotNull(entryPath, "entryPath");

        return this.createEntryWriteStream(ZipEntryParameters.create().setEntryPath(entryPath));
    }

    public ZipEntryWriteStream createEntryWriteStream(ZipEntryParameters parameters)
    {
        PreCondition.assertNotNull(parameters, "parameters");
        PreCondition.assertNotNull(parameters.getEntryPath(), "parameters.getEntryPath()");

        if (this.currentEntryWriteStream != null)
        {
            this.currentEntryWriteStream.dispose().await();
        }

        final java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(parameters.getEntryPath().toString());
        
        final String comment = parameters.getComment();
        if (comment != null)
        {
            zipEntry.setComment(parameters.getComment());
        }
        
        final DateTime lastModified = parameters.getLastModified();
        if (lastModified != null)
        {
            final Duration durationSinceEpoch = lastModified.getDurationSinceEpoch();
            final double millisecondsSinceEpoch = durationSinceEpoch.toMilliseconds().getValue();
            final java.nio.file.attribute.FileTime fileTime = java.nio.file.attribute.FileTime.fromMillis((long)millisecondsSinceEpoch);
            zipEntry.setLastModifiedTime(fileTime);
        }

        try
        {
            this.zipOutputStream.putNextEntry(zipEntry);
        }
        catch (Exception e)
        {
            throw Exceptions.asRuntime(e);
        }

        final ByteWriteStream byteWriteStream = OutputStreamToByteWriteStream.create(this.zipOutputStream);
        this.currentEntryWriteStream = ZipEntryWriteStream.create(byteWriteStream, this::closeCurrentEntry);
        final ZipEntryWriteStream result = this.currentEntryWriteStream;
        
        PostCondition.assertNotNull(result, "result");
        PostCondition.assertSame(result, this.currentEntryWriteStream, "this.currentEntryWriteStream");
        
        return result;
    }

    public ZipWriteStream createEntry(String entryPath, Action1<ZipEntryWriteStream> writeStreamAction)
    {
        PreCondition.assertNotNullAndNotEmpty(entryPath, "entryPath");
        PreCondition.assertNotNull(writeStreamAction, "writeStreamAction");

        return this.createEntry(Path.parse(entryPath), writeStreamAction);
    }

    public ZipWriteStream createEntry(Path entryPath, Action1<ZipEntryWriteStream> writeStreamAction)
    {
        PreCondition.assertNotNull(entryPath, "entryPath");
        PreCondition.assertNotNull(writeStreamAction, "writeStreamAction");

        return this.createEntry(ZipEntryParameters.create().setEntryPath(entryPath), writeStreamAction);
    }

    public ZipWriteStream createEntry(ZipEntryParameters parameters, Action1<ZipEntryWriteStream> writeStreamAction)
    {
        PreCondition.assertNotNull(parameters, "parameters");
        PreCondition.assertNotNull(writeStreamAction, "writeStreamAction");

        try (final ZipEntryWriteStream writeStream = this.createEntryWriteStream(parameters))
        {
            writeStreamAction.run(writeStream);
        }

        return this;
    }
}
