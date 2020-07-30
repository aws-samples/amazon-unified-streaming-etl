const AWS = require('aws-sdk');
var docClient = new AWS.DynamoDB.DocumentClient();

// console.log('Loading function');

exports.main = async (event) => {
    // /some other stuff to set up the variables below

    const promises = event.Records.map(record => {
        
        const payload = Buffer.from(record.kinesis.data, 'base64').toString('ascii');
        //console.log('Decoded payload:', payload);
        
        var textPayload = payload;
        var jto = JSON.parse(textPayload);
        
        
        var db_params = {
        TableName:"UnifiedOrderEnriched",
            Item:{
                "orderId": jto.orderId,
                "itemId": jto.itemId,
                "event_ts": jto.event_ts,
                "orderAmount": jto.orderAmount,
                "orderStatus": jto.orderStatus,
                "orderDateTime": jto.orderDateTime,
                "shipToName": jto.shipToName,
                "shipToAddress": jto.shipToAddress,
                "shipToCity": jto.shipToCity,
                "shipToState": jto.shipToState,
                "shipToZip": jto.shipToZip,
                "itemAmount": jto.itemAmount,
                "itemQuantity": jto.itemQuantity,
                "itemStatus": jto.itemStatus,
                "productName": jto.productName,


            }
        };

        return docClient
            .put(db_params)
            .promise()
            .then((item) => {
                // console.log('OrdersEnriched ' + ' inserted')
                // console.log(item)
                return item
            })
            .catch((error) => {
                console.log('ERROR: ')
                console.log(error)
                return error
            })
    })

    return Promise.all(promises)
}