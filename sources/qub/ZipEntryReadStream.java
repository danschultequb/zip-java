package qub;

public class ZipEntryReadStream implements CharacterToByteReadStream
{
    private final java.util.zip.ZipEntry zipEntry;
    private final CharacterToByteReadStream readStream;
    private boolean disposed;

    private ZipEntryReadStream(java.util.zip.ZipEntry zipEntry, CharacterToByteReadStream readStream)
    {
        PreCondition.assertNotNull(zipEntry, "zipEntry");
        PreCondition.assertNotNull(readStream, "readStream");

        this.zipEntry = zipEntry;
        this.readStream = readStream;
    }

    public static ZipEntryReadStream create(java.util.zip.ZipEntry zipEntry, CharacterToByteReadStream readStream)
    {
        return new ZipEntryReadStream(zipEntry, readStream);
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

                this.readStream.dispose().await();
            }
            return result;
        });
    }

    @Override
    public Result<Character> readCharacter()
    {
        PreCondition.assertNotDisposed(this, "this");
        
        return this.readStream.readCharacter();
    }

    @Override
    public Result<Byte> readByte()
    {
        PreCondition.assertNotDisposed(this, "this");
        
        return this.readStream.readByte();
    }

    @Override
    public Result<Integer> readBytes(byte[] outputBytes, int startIndex, int length)
    {
        PreCondition.assertNotDisposed(this, "this");

        return this.readStream.readBytes(outputBytes, startIndex, length);
    }

    @Override
    public CharacterEncoding getCharacterEncoding()
    {
        PreCondition.assertNotDisposed(this, "this");
        
        return this.readStream.getCharacterEncoding();
    }

    @Override
    public ZipEntryReadStream setCharacterEncoding(CharacterEncoding characterEncoding)
    {
        PreCondition.assertNotDisposed(this, "this");

        this.readStream.setCharacterEncoding(characterEncoding);

        return this;
    }

    /**
     * Get the path to this entry.
     */
    public Path getPath()
    {
        return Path.parse(this.zipEntry.getName());
    }

    /**
     * Get the comment string for this entry, or null if this entry doesn't have a comment.
     */
    public String getComment()
    {
        return this.zipEntry.getComment();
    }

    /**
     * Get whether this entry references a directory within the zip.
     */
    public boolean isDirectory()
    {
        return this.zipEntry.isDirectory();
    }

    public DataSize getCompressedSize()
    {
        return DataSize.bytes(this.zipEntry.getCompressedSize());
    }

    public DataSize getUncompressedSize()
    {
        return DataSize.bytes(this.zipEntry.getSize());
    }

    public DateTime getLastModified()
    {
        final java.nio.file.attribute.FileTime lastModifiedFileTime = this.zipEntry.getLastModifiedTime();
        final long millisecondsSinceEpoch = lastModifiedFileTime.toMillis();
        final Duration durationSinceEpoch = Duration.milliseconds(millisecondsSinceEpoch);
        final DateTime result = DateTime.createFromDurationSinceEpoch(durationSinceEpoch);

        PostCondition.assertNotNull(result, "result");

        return result;
    }
}
