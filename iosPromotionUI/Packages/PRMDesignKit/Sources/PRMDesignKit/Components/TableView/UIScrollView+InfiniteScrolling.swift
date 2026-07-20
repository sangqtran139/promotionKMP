import UIKit

private var infiniteScrollingViewKey: UInt8 = 0

public extension UIScrollView {
    var infiniteScrollingView: PRMInfiniteScrollingView? {
        get {
            return objc_getAssociatedObject(self, &infiniteScrollingViewKey) as? PRMInfiniteScrollingView
        }
        set {
            objc_setAssociatedObject(self, &infiniteScrollingViewKey, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
    
    func addInfiniteScrolling(actionHandler: @escaping () -> Void) {
        if infiniteScrollingView == nil {
            let view = PRMInfiniteScrollingView(frame: CGRect(x: 0, y: 0, width: bounds.width, height: 60))
            view.scrollView = self
            view.actionHandler = actionHandler
            
            if let tableView = self as? UITableView {
                let footerView = UIView(frame: view.bounds)
                footerView.addSubview(view)
                view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
                tableView.tableFooterView = footerView
            } else {
                addSubview(view)
                view.autoresizingMask = .flexibleWidth
            }
            
            infiniteScrollingView = view
        }
    }
    
    var showsInfiniteScrolling: Bool {
        get {
            return infiniteScrollingView?.isHidden == false
        }
        set {
            infiniteScrollingView?.isHidden = !newValue
        }
    }
}

public class PRMInfiniteScrollingView: UIView {
    public var actionHandler: (() -> Void)?
    public weak var scrollView: UIScrollView?
    
    private let activityIndicator = UIActivityIndicatorView(style: .gray)
    private var isObserving = false
    private var isAnimating = false
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }
    
    private func setupView() {
        addSubview(activityIndicator)
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            activityIndicator.centerXAnchor.constraint(equalTo: centerXAnchor),
            activityIndicator.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
    }
    
    public override func willMove(toSuperview newSuperview: UIView?) {
        super.willMove(toSuperview: newSuperview)
        if newSuperview == nil {
            removeObserver()
        } else {
            addObserver()
        }
    }
    
    private func addObserver() {
        guard !isObserving, let scrollView = scrollView else { return }
        scrollView.addObserver(self, forKeyPath: "contentOffset", options: .new, context: nil)
        isObserving = true
    }
    
    private func removeObserver() {
        guard isObserving, let scrollView = scrollView else { return }
        scrollView.removeObserver(self, forKeyPath: "contentOffset")
        isObserving = false
    }
    
    public override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "contentOffset" {
            scrollViewDidScroll()
        }
    }
    
    private func scrollViewDidScroll() {
        guard let scrollView = scrollView, !isAnimating, !isHidden else { return }
        
        let offsetY = scrollView.contentOffset.y
        let contentHeight = scrollView.contentSize.height
        let frameHeight = scrollView.frame.size.height
        
        if offsetY > 0 && offsetY + frameHeight >= contentHeight - 50 {
            startAnimating()
            actionHandler?()
        }
    }
    
    public func startAnimating() {
        guard !isAnimating else { return }
        isAnimating = true
        activityIndicator.startAnimating()
    }
    
    public func stopAnimating() {
        isAnimating = false
        activityIndicator.stopAnimating()
    }
}
