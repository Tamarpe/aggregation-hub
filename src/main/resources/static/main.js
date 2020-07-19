/**
 * Create HTML table row.
 *
 * @param {String} text The content to be placed inside the row.
 * @return {String} The content inside tr tags.
 */
function tr(text) {
    return '<tr>' + text + '</tr>';
}

/**
 * Create HTML table cell.
 *
 * @param {String|Number} text The content to be placed inside the cell.
 * @return {String} The content inside td tags.
 */
function td(text) {
    return '<td>' + text + '</td>';
}

/**
 * Create HTML table row.
 *
 * @param {Integer} rowId The row ID.
 * @param {String} errorCode The error code of the row.
 * @param {String} provider The provider of the row.
 * @param {String} products The products of the row.
 * @param {String} supportOpenCasesCnt The open support cases counter.
 * @param {String} supportCases All supported cases.
 * @return {String} The data inside a row.
 */
function row(rowId, errorCode, provider, products, supportOpenCasesCnt, supportCases) {
    return $(
        tr(
            td(rowId) +
            td(errorCode) +
            td(provider) +
            td(products) +
            td(supportOpenCasesCnt) +
            td(supportCases)
        ));
}

/**
 * Refresh the table data.
 */
function refreshTable() {
    let params = $('#searchCases').serialize();
    let url = '/search?' + params;

    $.get(url, function (data) {
        let mainTable = $('#allCases tbody');
        mainTable.empty();
        data.sort((a, b) => (a.errorCode) - (b.errorCode));
        let groups = groupByKeys(data, function (item) {
            return [item.provider, item.errorCode];
        });

        for (let i = 0; i < groups.length; i++) {
            let supportOpenCasesCnt = 0;
            let supportCases = [];
            let products = [];
            for (let j = 0; j < groups[i].length; j++) {
                if ((groups[i][j].status).toLowerCase() === 'open') {
                    supportOpenCasesCnt++;
                }
                let supportCase = ['<b>ID: ' + groups[i][j].caseId + '</b>', 'Status: ' + groups[i][j].status, 'Resource: ' + groups[i][j].resourceName];
                supportCase.push('</br> Created: ' + groups[i][j].creationDate);
                supportCase.push('Last modified: ' + groups[i][j].lastModifiedDate);
                supportCases.push(supportCase.join(', '));
                products.push(groups[i][j].productName);
            }
            mainTable.append(row(i + 1, groups[i][0].errorCode, groups[i][0].provider, products.join(', '), supportOpenCasesCnt, supportCases.join('</br>')));
        }
    });
}

/**
 * Group the data by keys.
 *
 * @param {array} data The data to be grouped.
 * @param {function} f The keys to group by.
 * @return {array} The data inside groups.
 */
function groupByKeys(data, f) {
    let groups = {};
    data.forEach(function (o) {
        let group = JSON.stringify(f(o));
        groups[group] = groups[group] || [];
        groups[group].push(o);
    });
    return Object.keys(groups).map(function (group) {
        return groups[group];
    })
}

$(document).ready(function () {
    refreshTable();
    $('#searchCases').validate({
        rules: {
            errorCode: {number: true},
            provider: {number: true},
            customerId: {number: true},
        },
        messages: {
            errorCode: {number: "The error code should contain numbers only"},
            provider: {number: "The provider should contain numbers only"},
            customerId: {number: "The customer ID should contain numbers only"}
        },
        submitHandler: function (form) {
            refreshTable();
        }
    });

    $('#refresh').click(function () {
        // An API call to refresh the data.
        $.get('/refresh', function () {
            refreshTable();
        });
    });

    $('#delete').click(function () {
        // An API call to delete the data.
        $.get('/delete', function () {
            refreshTable();
        });
    });

    // Clear all the values in the filters.
    $('#clear').click(function () {
        $("#searchCases input").each(function () {
            this.value = '';
        })
        $('#status').val('');
        refreshTable();
    });

});
