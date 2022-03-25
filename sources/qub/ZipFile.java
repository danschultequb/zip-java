package qub;

public class ZipFile extends File
{
    private ZipFile(File file)
    {
        super(file.getFileSystem(), file.getPath());
    }

    public static ZipFile get(File file)
    {
        PreCondition.assertNotNull(file, "file");

        return new ZipFile(file);
    }

    /**
     * Iterate over the entries in this {@link ZipFile}.
     * @return A {@link ZipEntryIterator} that will iterate over the entries in this
     * {@link ZipFile}.
     */
    public ZipEntryIterator iterateEntries()
    {
        return ZipEntryIterator.create(this);
    }

    public Result<ZipWriteStream> getContentsZipWriteStream()
    {
        return ZipWriteStream.create(this);
    }
}
