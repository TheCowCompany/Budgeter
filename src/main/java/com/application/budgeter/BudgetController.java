package com.application.budgeter;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;


public class BudgetController implements Initializable {
    
    @FXML AnchorPane budgetPage;
    @FXML private Label budgetTitle;

    // month section
    @FXML MenuButton monthMenu; 
    @FXML Label monthTitle; 

    // Budget Table
    @FXML TableView<Budget> BudgetTable;
    @FXML private TableColumn<Budget, String> category;
    @FXML private TableColumn<Budget,Double> total;
    @FXML private TableColumn<Budget,Double> spent;
    @FXML private TableColumn<Budget,Double> remaining;

    // Category text input and button
    @FXML private TextField limitTextField;
    @FXML private TextField categoryTextField;
    @FXML private Button categoryButton;

    // progress bar
    @FXML private ProgressBar SpendingBar;
    @FXML private Label progressTitle;

    // save budget button
    @FXML private Button saveBudgetButton;

    // data models
    BudgetModel budgetModel = new BudgetModel();
    ExpenseModel expenseModel = new ExpenseModel();

    ExpenseList expenseList;
    BudgetList budgetList;
    ObservableList<Budget> obsvBudgetList;

    //* set data models and setup elements that require data models
    public void setModels(ExpenseModel expenseModel, BudgetModel budgetModel) {
        // pass expenseList to MainPageController
        this.expenseModel = expenseModel;
        this.budgetModel = budgetModel;

        // budgetlist = most recent budgetlist
        ArrayList<String> dates = budgetModel.getDateList();
        String mostRecent = dates.get(dates.size() - 1);
        String year = mostRecent.substring(0,4);
        String month = mostRecent.substring(5);
        if (month.length() == 1) {month = "0" + month;}
        LocalDate date = LocalDate.parse(month + "/01/" + year, DateTimeFormatter.ofPattern("MM/dd/yyyy")); // create date object
        budgetList = budgetModel.getBudgetList(date);
        monthMenu.setText(mostRecent); // set monthMenu text to most recent budgetlist

        obsvBudgetList = budgetList.getBudgetList();

        LocalDate today = LocalDate.now();
        expenseList = expenseModel.getExpenseList(today); // get expenseList for current month
        
        BudgetTable.setItems(obsvBudgetList);
        updateSpending();
        setProgressBar();

        // delete menubutton context menu in BudgetTable
        ContextMenu contextMenu = new ContextMenu();
        BudgetTable.setContextMenu(contextMenu); // set context menu to tableview

        MenuItem deleteMenuItem = new MenuItem("Delete");
        contextMenu.getItems().addAll(deleteMenuItem);
        deleteListener(deleteMenuItem); 
    }

    @Override //* formatting page elements
    public void initialize(URL arg0, ResourceBundle arg1) {
        formatBudgetTable();
        setAnchorPaneConstraints();
    }


    //* add budget to budgetlist 
    public void submit(ActionEvent event) {
        try {
            String categoryName = categoryTextField.getText();
            double categoryLimit = Double.parseDouble(limitTextField.getText());

            Budget newBudget = new Budget(categoryName, 0, categoryLimit);

            budgetList.add(newBudget);

            categoryTextField.clear();
            limitTextField.clear();
        }
        catch (Exception e) {
            System.out.print(e);
        }
    } // end submit method


    //* listener for deleting budget from budgetlist
    private void deleteListener(MenuItem deleteMenuItem) {
        deleteMenuItem.setOnAction((ActionEvent event) -> {
            
            if (expenseList != null) {
                // if category is in expenseList, send alert and return
                for (Expense expense : expenseList) {
                    if (expense.getCategory().equals(BudgetTable.getSelectionModel().getSelectedItem().category)) {
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Error");
                        alert.setHeaderText("Cannot delete category that has expenses");
                        alert.setContentText("Please delete expenses in this category first");
                        alert.showAndWait();
                        return;
                    }
                }
            }
            // get selected row
            Budget selectedBudget = BudgetTable.getSelectionModel().getSelectedItem();
            // remove selected row from the data
            budgetList.remove(selectedBudget);
        });
    } // end deleteListener method


    //* update spending for each category by reading expenseList
    private void updateSpending() {
        // for each category, check category spending in expenseList
        for (Budget budget : budgetList.getBudgetList()) {
            double newSpent = expenseList.getCategorySpending(budget.category);
            Budget oldBudget = budget;
            Budget newBudget = new Budget(budget.category, newSpent, budget.total);

            if (newSpent != -1) { // if category is in expenseList
                budgetList.edit(oldBudget, newBudget);
            }
        }
    } // end updateSpending method


    //* write budget to csv file
    public void saveBudget() {
        budgetModel.saveAll();

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Saved");
        alert.setHeaderText("Budget saved");
        alert.setContentText("Your budget has been saved");
        alert.showAndWait();
    } // end saveBudget method


    //* set progress bar and title to percentage of budget spent
    private void setProgressBar() {
        double totalSpent = 0;
        double totalBudget = 0;
        for (Budget budget : budgetList.getBudgetList()) {
           totalSpent += budget.spent;
           totalBudget += budget.total;
        }
        SpendingBar.setProgress(totalSpent/totalBudget);

        // format totalSpent and totalBudget to 2 decimal places 
        String spent =  String.format("%.2f", totalSpent);
        String total = String.format("%.2f", totalBudget);

        progressTitle.setText("Spent: $" + spent + " / $" + total + " (" + (int)(totalSpent/totalBudget * 100) + "%)");
    } // end setProgressBar method
    
    
    //* apply formatting to table
    private void formatBudgetTable() {
        formatCurrencyColumns();

        BudgetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        category.setCellValueFactory(new PropertyValueFactory<Budget, String>("category"));
        total.setCellValueFactory(new PropertyValueFactory<Budget, Double>("total"));
        spent.setCellValueFactory(new PropertyValueFactory<Budget, Double>("spent"));
        remaining.setCellValueFactory(new PropertyValueFactory<Budget, Double>("remaining"));
    } // end formatBudgetTable


    //* format all currency columns
    private void formatCurrencyColumns() {
        formatCurrencyColumn(remaining);
        formatCurrencyColumn(spent);
        formatCurrencyColumn(total);
    } // end formatCurrencyColumns method


    //* add dollar sign and 2 decimal places to given column
    private void formatCurrencyColumn(TableColumn<Budget, Double> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else 
                    setText(String.format("$%.2f", item));
            }
        });
    } // end formatCurrencyColumn method


    //* set anchorpane constraints for all elements when window is resized
    private void setAnchorPaneConstraints() {
        // width property listener
        budgetPage.widthProperty().addListener((obs, oldVal, newVal) -> {
            setWidthConstraints(budgetTitle, newVal, 0.40, 0.40); 
            setWidthConstraints(BudgetTable, newVal, 0.10, 0.10); 
            setWidthConstraints(SpendingBar, newVal, 0.30, 0.30); 
            setWidthConstraints(progressTitle, newVal, 0.40, 0.40); 
            setWidthConstraints(categoryTextField, newVal, 0.275, 0.55);  
            setWidthConstraints(limitTextField, newVal, 0.475, 0.35); 
            setWidthConstraints(categoryButton, newVal, 0.675, 0.275);
            
            setWidthConstraints(monthMenu, newVal, .1, .79);
            setWidthConstraints(monthTitle, newVal, .1, .79);

            // save button at right 10% of window
            setWidthConstraints(saveBudgetButton, newVal, .8, .1);   
        });

        // height property listener
        budgetPage.heightProperty().addListener((obs, oldVal, newVal) -> {
            setHeightConstraints(BudgetTable, newVal, 0.40, 0.05);
            setHeightConstraints(SpendingBar, newVal, 0.20, 0.77);
            setHeightConstraints(budgetTitle, newVal, 0.03, 0.92);
            setHeightConstraints(progressTitle, newVal, 0.15, 0.80);
            AnchorPane.setTopAnchor(limitTextField, newVal.doubleValue() * .30);
            AnchorPane.setTopAnchor(categoryTextField, newVal.doubleValue() * .30);
            AnchorPane.setTopAnchor(categoryButton, newVal.doubleValue() * .30);

            AnchorPane.setTopAnchor(monthMenu, newVal.doubleValue() * .1);
            AnchorPane.setTopAnchor(monthTitle, newVal.doubleValue() * .075);

            AnchorPane.setBottomAnchor(saveBudgetButton, newVal.doubleValue() * .72); 
        });
    } // end setAnchorPaneConstraints method


    // set left and right anchor constraints
    private void setWidthConstraints(Node element, Number newVal,  double left, double right) {
        AnchorPane.setLeftAnchor(element, newVal.doubleValue() * left);
        AnchorPane.setRightAnchor(element, newVal.doubleValue() * right);
    } // end setWidthConstraints method


    // set top and bottom anchor constraints
    private void setHeightConstraints(Node element, Number newVal,  double top, double bottom) {
        AnchorPane.setTopAnchor(element, newVal.doubleValue() * top);
        AnchorPane.setBottomAnchor(element, newVal.doubleValue() * bottom);
    } // end setHeightConstraints method
} // end of budget controller class