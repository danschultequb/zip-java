package qub;

public class ZipEntryWriteStream implements CharacterToByteWriteStream
{
    private final CharacterToByteWriteStream innerStream;
    private final Action0 closeEntryAction;
    private boolean disposed;

    private ZipEntryWriteStream(CharacterToByteWriteStream innerStream, Action0 closeEntryAction)
    {
        PreCondition.assertNotNull(innerStream, "innerStream");
        PreCondition.assertNotNull(closeEntryAction, "closeEntryAction");

        this.innerStream = innerStream;
        this.closeEntryAction = closeEntryAction;
    }

    public static ZipEntryWriteStream create(ByteWriteStream byteWriteStream, Action0 closeEntryAction)
    {
        PreCondition.assertNotNull(byteWriteStream, "byteWriteStream");
        PreCondition.assertNotNull(closeEntryAction, "closeEntryAction");

        final CharacterToByteWriteStream innerStream = CharacterToByteWriteStream.create(byteWriteStream);
        return new ZipEntryWriteStream(innerStream, closeEntryAction);
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

                this.closeEntryAction.run();
            }
            return result;
        });
    }

    @Override
    public String getNewLine()
    {
        return this.innerStream.getNewLine();
    }

    @Override
    public Result<Integer> write(char toWrite)
    {
        PreCondition.assertNotDisposed(this, "this");

        return this.innerStream.write(toWrite);
    }

    @Override
    public Result<Integer> write(String toWrite, Object... formattedStringArguments)
    {
        PreCondition.assertNotDisposed(this, "this");

        return this.innerStream.write(toWrite, formattedStringArguments);
    }

    @Override
    public Result<Integer> write(byte toWrite)
    {
        PreCondition.assertNotDisposed(this, "this");

        return this.innerStream.write(toWrite);
    }

    @Override
    public Result<Integer> write(byte[] toWrite, int startIndex, int length)
    {
        PreCondition.assertNotDisposed(this, "this");

        return this.innerStream.write(toWrite, startIndex, length);
    }

    @Override
    public CharacterEncoding getCharacterEncoding()
    {
        return this.innerStream.getCharacterEncoding();
    }

    @Override
    public ZipEntryWriteStream setCharacterEncoding(CharacterEncoding characterEncoding)
    {
        PreCondition.assertNotDisposed(this, "this");

        this.innerStream.setCharacterEncoding(characterEncoding);

        return this;
    }

    @Override
    public ZipEntryWriteStream setNewLine(String newLine)
    {
        PreCondition.assertNotDisposed(this, "this");

        this.innerStream.setNewLine(newLine);

        return this;
    }
}
