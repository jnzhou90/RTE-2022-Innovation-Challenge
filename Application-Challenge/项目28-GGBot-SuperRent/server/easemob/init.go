package easemob

var DefaultClient *Client

func init() {
	DefaultClient, _ = NewClient(true)
}
