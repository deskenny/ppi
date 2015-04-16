# ppi
Property Price Index
Irish Property price index. To ensure an accurate payment, tax payers must self evaluate the value of their properties and submit this amount to revenue for calculation of the tax due. If the property is under valued the property owner will not know until in several years when they go to sell the property and the true value is established, they will be retrospectively fined.
Currently there are several sources of information for people who need to pay property tax. 
https://www.propertypriceregister.ie/website/npsra/pprweb.nsf/page/ppr-home-en
www.daft.ie/price-register  
http://www.myhome.ie/priceregister/ireland
http://www.propertyregister.ie/
The problem with all of these sources of information is that they only list, address, price, date of sale, 2nd hand or new, inclusive of VAT. They do not provide historic descriptions of the properties, square footage, condition, number of bedrooms, pictures etc. This information is necessary to truly establish the value of property. For example the property price index for Dublin 2015 lists “1 old county glen, crumlin, Dublin 12” as selling for E288,000 on 31st of March, but “165 leighlin road, crumlin, Dublin “ selling for E161,000. Looking at the brochures we see one is in “excellent condition”, while the other “would benefit from refurbishment”. Also one has 2 bedrooms, while the other has 3 bedrooms. Revenue have a form to help with this calculation https://lpt.revenue.ie/lpt-web/reckoner/lpt.html?locale=en
The purpose of this application is to daily query the data from the governments property price register looking for properties matching the users search criteria. When new properties become available query myhome, daft etc to get extra information to more accurately establish value. This information is then stored offline on the user’s phone, so that when they disappear off the myhome site the data is still available for the user. Geolocation information is also retrieved and used to map the sold houses for the user. The amount of time to keep this information will be configurable by the user, along with what period of time to search for and the address search string. This indirectly gives the user control of how many offline brochures are kept on the phone. As the queries are incremental, the amount of data transferred from internet to the phone is small.