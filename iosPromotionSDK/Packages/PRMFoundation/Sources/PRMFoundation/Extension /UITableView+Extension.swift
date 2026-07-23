import UIKit

public extension UITableView {
    
    /// Register a cell using its class name as the nib name and reuse identifier.
    /// - Parameters:
    ///   - cellClass: The class of the cell to register.
    ///   - bundle: The bundle containing the nib file. Defaults to the bundle of the cell class.
    func registerCell<T: UITableViewCell>(_ cellClass: T.Type, bundle: Bundle? = nil) {
        let identifier = String(describing: cellClass)
        let nibBundle = bundle ?? Bundle(for: cellClass)
        let nib = UINib(nibName: identifier, bundle: nibBundle)
        register(nib, forCellReuseIdentifier: identifier)
    }
    
    /// Dequeue a strongly typed cell.
    /// - Parameters:
    ///   - cellClass: The class of the cell to dequeue.
    ///   - indexPath: The index path specifying the location of the cell.
    /// - Returns: A strongly typed cell instance.
    func dequeueCell<T: UITableViewCell>(_ cellClass: T.Type, for indexPath: IndexPath) -> T {
        let identifier = String(describing: cellClass)
        guard let cell = dequeueReusableCell(withIdentifier: identifier, for: indexPath) as? T else {
            fatalError("Cannot dequeue cell with identifier: \(identifier)")
        }
        return cell
    }
}
