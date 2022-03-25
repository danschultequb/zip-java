package qub;

public class ZipEntryParameters
{
    private Path entryPath;
    private String comment;
    private DateTime lastModified;
    
    private ZipEntryParameters()
    {
    }

    public static ZipEntryParameters create()
    {
        return new ZipEntryParameters();
    }

    public Path getEntryPath()
    {
        return this.entryPath;
    }

    public ZipEntryParameters setEntryPath(String entryPath)
    {
        PreCondition.assertNotNullAndNotEmpty(entryPath, "entryPath");

        return this.setEntryPath(Path.parse(entryPath));
    }

    public ZipEntryParameters setEntryPath(Path entryPath)
    {
        PreCondition.assertNotNull(entryPath, "entryPath");

        this.entryPath = entryPath;

        return this;
    }

    public String getComment()
    {
        return this.comment;
    }

    public ZipEntryParameters setComment(String comment)
    {
        PreCondition.assertNotNullAndNotEmpty(comment, "comment");

        this.comment = comment;

        return this;
    }

    public DateTime getLastModified()
    {
        return this.lastModified;
    }

    public ZipEntryParameters setLastModified(DateTime lastModified)
    {
        PreCondition.assertNotNull(lastModified, "lastModified");

        this.lastModified = lastModified;

        return this;
    }

    public JSONObject toJson()
    {
        final JSONObject result = JSONObject.create();

        if (this.entryPath != null)
        {
            result.setString("entryPath", Objects.toString(this.entryPath));
        }

        if (this.comment != null)
        {
            result.setString("comment", this.comment);
        }

        if (this.lastModified != null)
        {
            result.setString("lastModified", this.lastModified.toString());
        }
        
        PostCondition.assertNotNull(result, "result");

        return result;
    }

    @Override
    public String toString()
    {
        return this.toJson().toString();
    }

    @Override
    public boolean equals(Object rhs)
    {
        return rhs instanceof ZipEntryParameters &&
            this.equals((ZipEntryParameters)rhs);
    }

    public boolean equals(ZipEntryParameters rhs)
    {
        return rhs != null &&
            Comparer.equal(this.entryPath, rhs.entryPath) &&
            Comparer.equal(this.comment, rhs.comment) &&
            Comparer.equal(this.lastModified, rhs.lastModified);
    }
}
