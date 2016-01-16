package eyes.blue;
public interface RemoteSource {
	public String getName();
	public String getMediaFileAddress(int i);
	public String getSubtitleFileAddress(int i);
	public String getTheoryFileAddress(int i);
	public String getGlobalLamrimSchedule();
}
